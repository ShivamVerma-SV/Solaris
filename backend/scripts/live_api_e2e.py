#!/usr/bin/env python3
"""Exercise the running Solaris API against PostgreSQL and Redis.

The script creates uniquely named fixtures, records every HTTP assertion, and
removes its database records before exiting. Existing application data is not
modified, except that system settings are temporarily changed and restored.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
BASE_URL = os.environ.get("SOLARIS_BASE_URL", "http://127.0.0.1:8080")
REPORT_PATH = ROOT / "output" / "api" / "live-api-test-report.json"
RUN_ID = f"{int(time.time())}-{os.getpid()}"
PREFIX = f"e2e-solaris-{RUN_ID}"
PASSWORD = "Live-Test-Password-42!"

ADMIN_EMAIL = f"{PREFIX}-admin@example.invalid"
OWNER_EMAIL = f"{PREFIX}-owner@example.invalid"
OWNER_UPDATED_EMAIL = f"{PREFIX}-owner-updated@example.invalid"
OTHER_EMAIL = f"{PREFIX}-other@example.invalid"
DELETED_EMAIL = f"{PREFIX}-deleted@example.invalid"
MANAGED_EMAIL = f"{PREFIX}-managed@example.invalid"

RESULTS: list[dict[str, Any]] = []
TOKENS: list[str] = []
ORIGINAL_SETTINGS: dict[str, Any] | None = None
ADMIN_ACCESS: str | None = None


def load_env() -> dict[str, str]:
    values = dict(os.environ)
    for raw_line in (ROOT / ".env").read_text().splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values.setdefault(key.strip(), value.strip().strip("'\""))
    return values


ENV = load_env()


def sql(statement: str, *, scalar: bool = False) -> str:
    command = [
        "docker", "compose", "exec", "-T", "postgres", "psql",
        "-v", "ON_ERROR_STOP=1", "-U", ENV["DB_USER"], "-d", ENV["DB_NAME"],
        "-Atc", statement,
    ]
    completed = subprocess.run(
        command, cwd=ROOT, check=True, capture_output=True, text=True, env=ENV
    )
    output = completed.stdout.strip()
    return output.splitlines()[0] if scalar and output else output


def json_body(response_body: bytes) -> Any:
    if not response_body:
        return None
    text = response_body.decode("utf-8", errors="replace")
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return text


def request(
    method: str,
    path: str,
    *,
    token: str | None = None,
    body: Any = None,
    raw_body: str | None = None,
    content_type: str = "application/json",
) -> tuple[int, Any, dict[str, str]]:
    headers = {"Accept": "application/json"}
    if token is not None:
        headers["Authorization"] = token if token.startswith("Bearer") else f"Bearer {token}"
    data = None
    if raw_body is not None:
        data = raw_body.encode()
        headers["Content-Type"] = content_type
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = content_type
    req = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status, json_body(response.read()), dict(response.headers)
    except urllib.error.HTTPError as error:
        return error.code, json_body(error.read()), dict(error.headers)


def check(
    name: str,
    method: str,
    path: str,
    expected: int | set[int],
    *,
    token: str | None = None,
    body: Any = None,
    raw_body: str | None = None,
    content_type: str = "application/json",
    predicate=None,
) -> Any:
    started = time.perf_counter()
    status, response, headers = request(
        method, path, token=token, body=body, raw_body=raw_body, content_type=content_type
    )
    expected_set = {expected} if isinstance(expected, int) else expected
    passed = status in expected_set and status < 500
    detail = None
    if passed and predicate is not None:
        try:
            passed = bool(predicate(response))
        except Exception as exception:  # noqa: BLE001 - report predicate failures clearly
            passed = False
            detail = f"predicate raised {type(exception).__name__}: {exception}"
        if not passed and detail is None:
            detail = "response predicate failed"
    if not passed and detail is None:
        detail = f"expected {sorted(expected_set)}, received {status}: {response!r}"
    RESULTS.append({
        "name": name,
        "method": method,
        "path": path,
        "expectedStatus": sorted(expected_set),
        "actualStatus": status,
        "passed": passed,
        "durationMs": round((time.perf_counter() - started) * 1000, 2),
        "detail": detail,
    })
    print(f"{'PASS' if passed else 'FAIL'} {status:3d} {method:6s} {path} - {name}")
    if not passed:
        raise AssertionError(detail)
    return response


def login(email: str) -> dict[str, Any]:
    result = check(
        f"login {email.split('@')[0]}", "POST", "/api/auth/login", 200,
        body={"email": email, "password": PASSWORD},
        predicate=lambda value: value["tokenType"] == "Bearer" and value["accessToken"],
    )
    TOKENS.append(result["refreshToken"])
    return result


def register(email: str, name: str) -> None:
    check(
        f"register {name}", "POST", "/api/auth/register", 201,
        body={"name": name, "email": email, "password": PASSWORD},
    )


def b64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode()


def expired_access_token(user_id: int, email: str, role: str, reference_token: str) -> str:
    algorithm = json.loads(base64.urlsafe_b64decode(reference_token.split(".")[0] + "=="))["alg"]
    digest = {"HS256": hashlib.sha256, "HS384": hashlib.sha384, "HS512": hashlib.sha512}[algorithm]
    now = int(time.time())
    header = b64url(json.dumps({"alg": algorithm}, separators=(",", ":")).encode())
    payload = b64url(json.dumps({
        "sub": email, "role": role, "userId": user_id, "type": "ACCESS",
        "iat": now - 120, "exp": now - 60,
    }, separators=(",", ":")).encode())
    signature = b64url(hmac.new(ENV["JWT_SECRET"].encode(), f"{header}.{payload}".encode(), digest).digest())
    return f"{header}.{payload}.{signature}"


def auth_payload(email: str, name: str, role: str = "HOMEOWNER", enabled: bool = True) -> dict[str, Any]:
    return {
        "name": name,
        "email": email,
        "password": PASSWORD,
        "role": role,
        "phone": "+1 555 0100",
        "enabled": enabled,
    }


def settings_payload(source: dict[str, Any], **overrides: Any) -> dict[str, Any]:
    keys = [
        "lowBatteryThreshold", "highConsumptionThresholdKwh", "lowProductionThresholdKwh",
        "productionAlertsEnabled", "consumptionAlertsEnabled", "batteryAlertsEnabled",
        "deviceOfflineAlertEnabled", "deviceOfflineThresholdMinutes", "emailNotificationsEnabled",
    ]
    result = {key: source[key] for key in keys}
    result.update(overrides)
    return result


def run() -> None:
    global ADMIN_ACCESS, ORIGINAL_SETTINGS

    check("health through protected route", "GET", "/api/admin/users", 401)
    check("malformed bearer", "GET", "/api/admin/users", 401, token="Bearer")
    check("invalid JWT", "GET", "/api/admin/users", 401, token="not.a.jwt")
    check("malformed JSON", "POST", "/api/auth/register", 400, raw_body="{not-json")
    check("missing registration fields", "POST", "/api/auth/register", 400, body={})
    check("invalid registration email", "POST", "/api/auth/register", 400,
          body={"name": "Bad Email", "email": "not-an-email", "password": PASSWORD})
    check("short registration password", "POST", "/api/auth/register", 400,
          body={"name": "Short Password", "email": f"{PREFIX}-short@example.invalid", "password": "short"})
    check("method not supported", "GET", "/api/auth/login", 405)

    register(ADMIN_EMAIL, "E2E Admin")
    register(OWNER_EMAIL, "E2E Owner")
    register(OTHER_EMAIL, "E2E Other Owner")
    check("duplicate public registration", "POST", "/api/auth/register", 409,
          body={"name": "Duplicate", "email": OWNER_EMAIL.upper(), "password": PASSWORD})

    sql(f"UPDATE users SET role='ADMIN' WHERE email='{ADMIN_EMAIL}'")
    admin = login(ADMIN_EMAIL)
    owner = login(OWNER_EMAIL)
    other = login(OTHER_EMAIL)
    ADMIN_ACCESS = admin["accessToken"]

    check("wrong password", "POST", "/api/auth/login", 401,
          body={"email": OWNER_EMAIL, "password": "incorrect-password"})
    check("unknown login", "POST", "/api/auth/login", 401,
          body={"email": f"{PREFIX}-unknown@example.invalid", "password": PASSWORD})
    check("expired access token", "GET", "/api/homeowner/profile", 401,
          token=expired_access_token(owner["userId"], OWNER_EMAIL, "HOMEOWNER", owner["accessToken"]))
    check("refresh token cannot access API", "GET", "/api/homeowner/profile", 401,
          token=owner["refreshToken"])
    check("access token cannot refresh", "POST", "/api/auth/refresh", 401,
          body={"refreshToken": owner["accessToken"]})
    check("homeowner cannot access admin", "GET", "/api/admin/users", 403,
          token=owner["accessToken"])
    check("admin cannot access homeowner", "GET", "/api/homeowner/profile", 403,
          token=ADMIN_ACCESS)

    rotated = check("rotate refresh token", "POST", "/api/auth/refresh", 200,
                    body={"refreshToken": other["refreshToken"]})
    check("rotated refresh replay", "POST", "/api/auth/refresh", 401,
          body={"refreshToken": other["refreshToken"]})
    check("logout rotated token", "POST", "/api/auth/logout", 204,
          body={"refreshToken": rotated["refreshToken"]})
    check("revoked refresh token", "POST", "/api/auth/refresh", 401,
          body={"refreshToken": rotated["refreshToken"]})
    other = login(OTHER_EMAIL)

    register(DELETED_EMAIL, "Deleted User")
    deleted = login(DELETED_EMAIL)
    sql(f"DELETE FROM users WHERE email='{DELETED_EMAIL}'")
    check("deleted user refresh", "POST", "/api/auth/refresh", 401,
          body={"refreshToken": deleted["refreshToken"]})

    # Admin user API
    check("list users", "GET", "/api/admin/users?page=0&size=20", 200, token=ADMIN_ACCESS,
          predicate=lambda value: isinstance(value["content"], list))
    check("get admin user", "GET", f"/api/admin/users/{admin['userId']}", 200, token=ADMIN_ACCESS,
          predicate=lambda value: "password" not in value)
    managed = check("create managed user", "POST", "/api/admin/users", 201,
                    token=ADMIN_ACCESS, body=auth_payload(MANAGED_EMAIL, "Managed User"))
    check("duplicate admin-created user", "POST", "/api/admin/users", 409,
          token=ADMIN_ACCESS, body=auth_payload(MANAGED_EMAIL.upper(), "Duplicate User"))
    check("invalid user role", "POST", "/api/admin/users", 400, token=ADMIN_ACCESS,
          body={**auth_payload(f"{PREFIX}-invalid-role@example.invalid", "Invalid Role"), "role": "SUPERUSER"})
    check("missing user fields", "POST", "/api/admin/users", 400, token=ADMIN_ACCESS, body={})
    update_managed = auth_payload(MANAGED_EMAIL, "Managed User Updated")
    update_managed["password"] = None
    update_managed["phone"] = None
    check("update managed user", "PUT", f"/api/admin/users/{managed['id']}", 200,
          token=ADMIN_ACCESS, body=update_managed)
    check("missing user", "GET", "/api/admin/users/9223372036854775807", 404, token=ADMIN_ACCESS)
    check("invalid user ID", "GET", "/api/admin/users/not-a-number", 400, token=ADMIN_ACCESS)
    check("invalid user pagination", "GET", "/api/admin/users?page=-1&size=101", 400, token=ADMIN_ACCESS)
    check("admin cannot self-delete", "DELETE", f"/api/admin/users/{admin['userId']}", 403,
          token=ADMIN_ACCESS)

    # Disabled user behavior
    disable_other = auth_payload(OTHER_EMAIL, "E2E Other Owner", enabled=False)
    disable_other["password"] = None
    check("disable homeowner", "PUT", f"/api/admin/users/{other['userId']}", 200,
          token=ADMIN_ACCESS, body=disable_other)
    check("disabled user access", "GET", "/api/homeowner/profile", 401, token=other["accessToken"])
    check("disabled user refresh", "POST", "/api/auth/refresh", 401,
          body={"refreshToken": other["refreshToken"]})
    enable_other = {**disable_other, "enabled": True}
    check("re-enable homeowner", "PUT", f"/api/admin/users/{other['userId']}", 200,
          token=ADMIN_ACCESS, body=enable_other)
    other = login(OTHER_EMAIL)

    # Profile behavior; logout the original refresh token before changing its subject email.
    check("get homeowner profile", "GET", "/api/homeowner/profile", 200, token=owner["accessToken"])
    check("duplicate profile email", "PUT", "/api/homeowner/profile", 409, token=owner["accessToken"],
          body={"name": "E2E Owner", "email": OTHER_EMAIL, "phone": "+1 555 0101"})
    check("logout original owner token", "POST", "/api/auth/logout", 204,
          body={"refreshToken": owner["refreshToken"]})
    check("update homeowner profile", "PUT", "/api/homeowner/profile", 200, token=owner["accessToken"],
          body={"name": "E2E Owner Updated", "email": OWNER_UPDATED_EMAIL, "phone": "+1 555 0102"})
    check("email change invalidates old access", "GET", "/api/homeowner/profile", 401,
          token=owner["accessToken"])
    owner = login(OWNER_UPDATED_EMAIL)

    # Sites
    site_body = {"ownerId": owner["userId"], "code": f"{PREFIX}-SITE-A", "name": "E2E Home A",
                 "address": "1 Solar Way", "capacityKw": 8.5, "active": True}
    other_site_body = {"ownerId": other["userId"], "code": f"{PREFIX}-SITE-B", "name": "E2E Home B",
                       "address": "2 Solar Way", "capacityKw": 6.0, "active": True}
    site = check("create site", "POST", "/api/admin/sites", 201, token=ADMIN_ACCESS, body=site_body)
    other_site = check("create other-owner site", "POST", "/api/admin/sites", 201,
                       token=ADMIN_ACCESS, body=other_site_body)
    disposable_site = check("create disposable site", "POST", "/api/admin/sites", 201, token=ADMIN_ACCESS,
                            body={**site_body, "code": f"{PREFIX}-SITE-T", "name": "Disposable Site"})
    check("duplicate site code", "POST", "/api/admin/sites", 409, token=ADMIN_ACCESS,
          body={**site_body, "name": "Duplicate Site"})
    check("site owner must be homeowner", "POST", "/api/admin/sites", 400, token=ADMIN_ACCESS,
          body={**site_body, "ownerId": admin["userId"], "code": f"{PREFIX}-BAD-OWNER"})
    check("missing site owner", "POST", "/api/admin/sites", 404, token=ADMIN_ACCESS,
          body={**site_body, "ownerId": 9223372036854775807, "code": f"{PREFIX}-NO-OWNER"})
    check("list sites", "GET", "/api/admin/sites?page=0&size=10", 200, token=ADMIN_ACCESS)
    check("get site", "GET", f"/api/admin/sites/{site['id']}", 200, token=ADMIN_ACCESS)
    check("update site", "PUT", f"/api/admin/sites/{site['id']}", 200, token=ADMIN_ACCESS,
          body={**site_body, "name": "E2E Home A Updated"})
    check("missing site", "GET", "/api/admin/sites/9223372036854775807", 404, token=ADMIN_ACCESS)
    check("homeowner list sites", "GET", "/api/homeowner/sites", 200, token=owner["accessToken"],
          predicate=lambda value: all(item["ownerId"] == owner["userId"] for item in value))
    check("homeowner get own site", "GET", f"/api/homeowner/sites/{site['id']}", 200,
          token=owner["accessToken"])
    check("homeowner cannot get foreign site", "GET", f"/api/homeowner/sites/{other_site['id']}", 404,
          token=owner["accessToken"])

    # Devices
    now = datetime.now(UTC).replace(microsecond=0)
    device_body = {"identifier": f"{PREFIX}-INV-A", "name": "Roof Inverter", "type": "SOLAR_INVERTER",
                   "status": "ONLINE", "siteId": site["id"], "manufacturer": "Solaris Test",
                   "model": "INV-1", "firmwareVersion": "1.0.0", "lastSeenAt": now.isoformat()}
    other_device_body = {**device_body, "identifier": f"{PREFIX}-INV-B", "name": "Other Inverter",
                         "siteId": other_site["id"]}
    device = check("create device", "POST", "/api/admin/devices", 201, token=ADMIN_ACCESS, body=device_body)
    other_device = check("create foreign device", "POST", "/api/admin/devices", 201,
                         token=ADMIN_ACCESS, body=other_device_body)
    disposable_device = check("create disposable device", "POST", "/api/admin/devices", 201,
                              token=ADMIN_ACCESS,
                              body={**device_body, "identifier": f"{PREFIX}-INV-T", "name": "Disposable Device"})
    check("duplicate device identifier", "POST", "/api/admin/devices", 409,
          token=ADMIN_ACCESS, body={**device_body, "name": "Duplicate Device"})
    check("invalid device enum", "POST", "/api/admin/devices", 400, token=ADMIN_ACCESS,
          body={**device_body, "identifier": f"{PREFIX}-INV-X", "type": "INVALID"})
    check("missing device site", "POST", "/api/admin/devices", 404, token=ADMIN_ACCESS,
          body={**device_body, "identifier": f"{PREFIX}-INV-N", "siteId": 9223372036854775807})
    check("list filtered devices", "GET",
          f"/api/admin/devices?status=ONLINE&type=SOLAR_INVERTER&siteId={site['id']}&page=0&size=10",
          200, token=ADMIN_ACCESS)
    check("get device", "GET", f"/api/admin/devices/{device['id']}", 200, token=ADMIN_ACCESS)
    check("update device", "PUT", f"/api/admin/devices/{device['id']}", 200, token=ADMIN_ACCESS,
          body={**device_body, "name": "Roof Inverter Updated"})
    check("missing device", "GET", "/api/admin/devices/9223372036854775807", 404, token=ADMIN_ACCESS)
    check("invalid device filter", "GET", "/api/admin/devices?status=INVALID", 400, token=ADMIN_ACCESS)

    # Batteries
    battery_body = {"identifier": f"{PREFIX}-BAT-A", "name": "Garage Battery", "siteId": site["id"],
                    "capacityKwh": 13.5, "status": "ONLINE", "currentChargePercent": 65,
                    "currentStoredEnergyKwh": 8.775, "lastUpdatedAt": now.isoformat()}
    other_battery_body = {**battery_body, "identifier": f"{PREFIX}-BAT-B", "name": "Other Battery",
                          "siteId": other_site["id"]}
    battery = check("create battery", "POST", "/api/admin/batteries", 201,
                    token=ADMIN_ACCESS, body=battery_body)
    other_battery = check("create foreign battery", "POST", "/api/admin/batteries", 201,
                          token=ADMIN_ACCESS, body=other_battery_body)
    disposable_battery = check("create disposable battery", "POST", "/api/admin/batteries", 201,
                               token=ADMIN_ACCESS,
                               body={**battery_body, "identifier": f"{PREFIX}-BAT-T", "name": "Disposable Battery"})
    check("duplicate battery identifier", "POST", "/api/admin/batteries", 409,
          token=ADMIN_ACCESS, body={**battery_body, "name": "Duplicate Battery"})
    check("battery stored energy exceeds capacity", "POST", "/api/admin/batteries", 400,
          token=ADMIN_ACCESS,
          body={**battery_body, "identifier": f"{PREFIX}-BAT-X", "capacityKwh": 5, "currentStoredEnergyKwh": 6})
    check("invalid battery enum", "POST", "/api/admin/batteries", 400, token=ADMIN_ACCESS,
          body={**battery_body, "identifier": f"{PREFIX}-BAT-I", "status": "INVALID"})
    check("missing battery site", "POST", "/api/admin/batteries", 404, token=ADMIN_ACCESS,
          body={**battery_body, "identifier": f"{PREFIX}-BAT-N", "siteId": 9223372036854775807})
    check("list batteries", "GET", "/api/admin/batteries?page=0&size=10", 200, token=ADMIN_ACCESS)
    check("get battery", "GET", f"/api/admin/batteries/{battery['id']}", 200, token=ADMIN_ACCESS)
    check("update battery", "PUT", f"/api/admin/batteries/{battery['id']}", 200,
          token=ADMIN_ACCESS, body={**battery_body, "name": "Garage Battery Updated"})
    check("missing battery", "GET", "/api/admin/batteries/9223372036854775807", 404, token=ADMIN_ACCESS)

    # Settings are restored in finally even if a later assertion fails.
    ORIGINAL_SETTINGS = check("get settings", "GET", "/api/admin/settings", 200, token=ADMIN_ACCESS)
    changed_settings = settings_payload(ORIGINAL_SETTINGS, lowBatteryThreshold=21.5,
                                        deviceOfflineThresholdMinutes=45)
    check("update settings", "PUT", "/api/admin/settings", 200,
          token=ADMIN_ACCESS, body=changed_settings)
    check("invalid settings", "PUT", "/api/admin/settings", 400, token=ADMIN_ACCESS,
          body={**changed_settings, "lowBatteryThreshold": 101, "deviceOfflineThresholdMinutes": 0})
    check("homeowner cannot read settings", "GET", "/api/admin/settings", 403,
          token=owner["accessToken"])

    # Telemetry is ingest-only in this version, so direct inserts provide realistic read fixtures.
    energy_id = int(sql(
        "INSERT INTO energy_readings "
        "(production_kwh,consumption_kwh,grid_import_kwh,grid_export_kwh,recorded_at,device_id,site_id) "
        f"VALUES (12.500,7.250,1.100,6.350,now(),{device['id']},{site['id']}) RETURNING id", scalar=True))
    sql(
        "INSERT INTO energy_readings "
        "(production_kwh,consumption_kwh,grid_import_kwh,grid_export_kwh,recorded_at,device_id,site_id) "
        f"VALUES (99.000,88.000,77.000,66.000,now(),{other_device['id']},{other_site['id']})")
    storage_id = int(sql(
        "INSERT INTO storage_readings (charge_percent,stored_energy_kwh,recorded_at,battery_id,site_id) "
        f"VALUES (64.50,8.700,now(),{battery['id']},{site['id']}) RETURNING id", scalar=True))
    sql(
        "INSERT INTO storage_readings (charge_percent,stored_energy_kwh,recorded_at,battery_id,site_id) "
        f"VALUES (33.00,4.000,now(),{other_battery['id']},{other_site['id']})")
    alert_id = int(sql(
        "INSERT INTO alerts (created_at,message,is_read,severity,type,battery_id,device_id,site_id,user_id) "
        f"VALUES (now(),'E2E low battery',false,'WARNING','LOW_BATTERY',{battery['id']},NULL,{site['id']},{owner['userId']}) "
        "RETURNING id", scalar=True))
    other_alert_id = int(sql(
        "INSERT INTO alerts (created_at,message,is_read,severity,type,battery_id,device_id,site_id,user_id) "
        f"VALUES (now(),'E2E other alert',false,'WARNING','LOW_BATTERY',{other_battery['id']},NULL,{other_site['id']},{other['userId']}) "
        "RETURNING id", scalar=True))

    from_time = urllib.parse.quote((now - timedelta(days=1)).isoformat())
    to_time = urllib.parse.quote((now + timedelta(minutes=5)).isoformat())
    check("energy summary", "GET",
          f"/api/homeowner/energy/summary?siteId={site['id']}&from={from_time}&to={to_time}", 200,
          token=owner["accessToken"], predicate=lambda value: value["readingCount"] >= 1)
    readings = check("energy readings", "GET",
                     f"/api/homeowner/energy/readings?siteId={site['id']}&from={from_time}&to={to_time}&page=0&size=20",
                     200, token=owner["accessToken"])
    if not any(item["id"] == energy_id for item in readings["content"]):
        raise AssertionError("own energy fixture missing from response")
    if any(item["siteId"] == other_site["id"] for item in readings["content"]):
        raise AssertionError("foreign energy leaked into homeowner response")
    check("foreign energy site", "GET", f"/api/homeowner/energy/summary?siteId={other_site['id']}", 404,
          token=owner["accessToken"])
    check("invalid energy time order", "GET",
          f"/api/homeowner/energy/summary?from={to_time}&to={from_time}", 400, token=owner["accessToken"])
    check("oversized energy range", "GET",
          "/api/homeowner/energy/readings?from=2020-01-01T00%3A00%3A00Z&to=2022-01-01T00%3A00%3A00Z",
          400, token=owner["accessToken"])
    check("malformed energy timestamp", "GET", "/api/homeowner/energy/readings?from=not-a-date", 400,
          token=owner["accessToken"])
    check("invalid energy pagination", "GET", "/api/homeowner/energy/readings?page=-1&size=201", 400,
          token=owner["accessToken"])
    check("empty energy range", "GET",
          "/api/homeowner/energy/readings?from=2020-01-01T00%3A00%3A00Z&to=2020-01-02T00%3A00%3A00Z",
          200, token=owner["accessToken"], predicate=lambda value: value["totalElements"] == 0)

    today = now.date().isoformat()
    check("daily JDBC energy report", "GET",
          f"/api/homeowner/reports/energy?siteId={site['id']}&from={today}&to={today}", 200,
          token=owner["accessToken"], predicate=lambda value: len(value) >= 1)
    check("foreign report site", "GET", f"/api/homeowner/reports/energy?siteId={other_site['id']}", 404,
          token=owner["accessToken"])
    check("invalid report range", "GET", "/api/homeowner/reports/energy?from=2026-02-02&to=2026-02-01", 400,
          token=owner["accessToken"])
    check("oversized report range", "GET", "/api/homeowner/reports/energy?from=2020-01-01&to=2022-01-02", 400,
          token=owner["accessToken"])

    status = check("storage status", "GET", "/api/homeowner/storage/status", 200,
                   token=owner["accessToken"])
    if any(item["ownerId"] != owner["userId"] for item in status):
        raise AssertionError("foreign battery leaked into status response")
    history = check("storage history", "GET",
                    f"/api/homeowner/storage/history?batteryId={battery['id']}&from={from_time}&to={to_time}&page=0&size=20",
                    200, token=owner["accessToken"])
    if not any(item["id"] == storage_id for item in history["content"]):
        raise AssertionError("own storage fixture missing from response")
    check("foreign battery history", "GET",
          f"/api/homeowner/storage/history?batteryId={other_battery['id']}", 404, token=owner["accessToken"])
    check("invalid storage time order", "GET",
          f"/api/homeowner/storage/history?from={to_time}&to={from_time}", 400, token=owner["accessToken"])
    check("invalid storage pagination", "GET", "/api/homeowner/storage/history?page=-1&size=201", 400,
          token=owner["accessToken"])

    homeowner_alerts = check("homeowner alerts", "GET", "/api/homeowner/alerts?read=false&page=0&size=20", 200,
                             token=owner["accessToken"])
    if not any(item["id"] == alert_id for item in homeowner_alerts["content"]):
        raise AssertionError("own alert fixture missing from response")
    if any(item["id"] == other_alert_id for item in homeowner_alerts["content"]):
        raise AssertionError("foreign alert leaked into homeowner response")
    check("mark own alert read", "PUT", f"/api/homeowner/alerts/{alert_id}/read", 200,
          token=owner["accessToken"], predicate=lambda value: value["read"] is True)
    check("mark foreign alert read", "PUT", f"/api/homeowner/alerts/{other_alert_id}/read", 404,
          token=owner["accessToken"])
    check("missing homeowner alert", "PUT", "/api/homeowner/alerts/9223372036854775807/read", 404,
          token=owner["accessToken"])
    check("invalid homeowner alert pagination", "GET", "/api/homeowner/alerts?page=-1&size=101", 400,
          token=owner["accessToken"])
    check("admin alerts with filters", "GET",
          f"/api/admin/alerts?type=LOW_BATTERY&severity=WARNING&read=false&userId={other['userId']}&page=0&size=20",
          200, token=ADMIN_ACCESS)
    check("invalid admin alert enum", "GET", "/api/admin/alerts?type=INVALID", 400, token=ADMIN_ACCESS)
    check("invalid admin alert pagination", "GET", "/api/admin/alerts?page=-1&size=101", 400,
          token=ADMIN_ACCESS)

    # Dependency and ownership safety
    check("cannot transfer site with dependents", "PUT", f"/api/admin/sites/{site['id']}", 409,
          token=ADMIN_ACCESS, body={**site_body, "ownerId": other["userId"]})
    check("cannot move device with telemetry", "PUT", f"/api/admin/devices/{device['id']}", 409,
          token=ADMIN_ACCESS, body={**device_body, "siteId": other_site["id"]})
    check("cannot delete device with telemetry", "DELETE", f"/api/admin/devices/{device['id']}", 409,
          token=ADMIN_ACCESS)
    check("cannot move battery with history", "PUT", f"/api/admin/batteries/{battery['id']}", 409,
          token=ADMIN_ACCESS, body={**battery_body, "siteId": other_site["id"]})
    check("cannot delete battery with history", "DELETE", f"/api/admin/batteries/{battery['id']}", 409,
          token=ADMIN_ACCESS)
    check("cannot delete site with dependents", "DELETE", f"/api/admin/sites/{site['id']}", 409,
          token=ADMIN_ACCESS)

    check("delete dependency-free device", "DELETE", f"/api/admin/devices/{disposable_device['id']}", 204,
          token=ADMIN_ACCESS)
    check("delete dependency-free battery", "DELETE", f"/api/admin/batteries/{disposable_battery['id']}", 204,
          token=ADMIN_ACCESS)
    check("delete dependency-free site", "DELETE", f"/api/admin/sites/{disposable_site['id']}", 204,
          token=ADMIN_ACCESS)
    check("delete managed user", "DELETE", f"/api/admin/users/{managed['id']}", 204, token=ADMIN_ACCESS)
    check("authenticated unknown endpoint", "GET", "/api/does-not-exist", 404, token=ADMIN_ACCESS)


def cleanup() -> None:
    if ORIGINAL_SETTINGS is not None and ADMIN_ACCESS is not None:
        try:
            request("PUT", "/api/admin/settings", token=ADMIN_ACCESS, body=settings_payload(ORIGINAL_SETTINGS))
        except Exception as exception:  # noqa: BLE001
            print(f"WARN could not restore settings through API: {exception}", file=sys.stderr)
    if ADMIN_ACCESS is not None:
        for refresh_token in TOKENS:
            try:
                request("POST", "/api/auth/logout", body={"refreshToken": refresh_token})
            except Exception:
                pass

    emails = [ADMIN_EMAIL, OWNER_EMAIL, OWNER_UPDATED_EMAIL, OTHER_EMAIL, DELETED_EMAIL, MANAGED_EMAIL]
    quoted = ",".join(f"'{email}'" for email in emails)
    try:
        sql(
            "BEGIN; "
            f"DELETE FROM alerts WHERE user_id IN (SELECT id FROM users WHERE email IN ({quoted})); "
            f"DELETE FROM storage_readings WHERE site_id IN (SELECT s.id FROM solar_sites s JOIN users u ON u.id=s.owner_id WHERE u.email IN ({quoted})); "
            f"DELETE FROM energy_readings WHERE site_id IN (SELECT s.id FROM solar_sites s JOIN users u ON u.id=s.owner_id WHERE u.email IN ({quoted})); "
            f"DELETE FROM batteries WHERE site_id IN (SELECT s.id FROM solar_sites s JOIN users u ON u.id=s.owner_id WHERE u.email IN ({quoted})); "
            f"DELETE FROM devices WHERE site_id IN (SELECT s.id FROM solar_sites s JOIN users u ON u.id=s.owner_id WHERE u.email IN ({quoted})); "
            f"DELETE FROM solar_sites WHERE owner_id IN (SELECT id FROM users WHERE email IN ({quoted})); "
            f"DELETE FROM users WHERE email IN ({quoted}); COMMIT;"
        )
    except Exception as exception:  # noqa: BLE001
        print(f"WARN fixture cleanup failed: {exception}", file=sys.stderr)


def write_report(success: bool, failure: str | None = None) -> None:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report = {
        "generatedAt": datetime.now(UTC).isoformat(),
        "baseUrl": BASE_URL,
        "runId": RUN_ID,
        "success": success,
        "summary": {
            "total": len(RESULTS),
            "passed": sum(1 for result in RESULTS if result["passed"]),
            "failed": sum(1 for result in RESULTS if not result["passed"]),
            "serverErrors": sum(1 for result in RESULTS if result["actualStatus"] >= 500),
        },
        "failure": failure,
        "results": RESULTS,
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    failure_message = None
    succeeded = False
    try:
        run()
        succeeded = True
    except Exception as exception:  # noqa: BLE001
        failure_message = f"{type(exception).__name__}: {exception}"
        print(f"\nFAILED: {failure_message}", file=sys.stderr)
    finally:
        cleanup()
        write_report(succeeded, failure_message)
    print(f"\nReport: {REPORT_PATH}")
    print(f"Passed: {sum(1 for result in RESULTS if result['passed'])}/{len(RESULTS)}")
    sys.exit(0 if succeeded else 1)
