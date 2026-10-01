#!/usr/bin/env python3
"""Generate the Solaris API guide for Django developers."""

from __future__ import annotations

import json
from html import escape
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    LongTable,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "output" / "pdf" / "Solaris_API_Guide_for_Django_Developers.pdf"
REPORT = ROOT / "output" / "api" / "live-api-test-report.json"

NAVY = colors.HexColor("#16324F")
BLUE = colors.HexColor("#245A7A")
ORANGE = colors.HexColor("#F29F05")
GOLD = colors.HexColor("#FFD166")
GREEN = colors.HexColor("#2D936C")
RED = colors.HexColor("#B94A48")
INK = colors.HexColor("#263238")
MUTED = colors.HexColor("#607D8B")
PALE = colors.HexColor("#F5F8FA")
PALE_BLUE = colors.HexColor("#EAF3F8")
PALE_ORANGE = colors.HexColor("#FFF6E4")
LINE = colors.HexColor("#D6E0E5")


class SolarisDocTemplate(BaseDocTemplate):
    def __init__(self, filename: str):
        super().__init__(
            filename,
            pagesize=A4,
            leftMargin=17 * mm,
            rightMargin=17 * mm,
            topMargin=19 * mm,
            bottomMargin=17 * mm,
            title="Solaris API Guide for Django Developers",
            author="Solaris Backend",
            subject="Endpoint, security, architecture, and implementation reference",
        )
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="main")
        self.addPageTemplates(PageTemplate(id="content", frames=[frame], onPage=self.draw_page))

    def draw_page(self, canvas, doc):
        canvas.saveState()
        width, height = A4
        canvas.setFillColor(NAVY)
        canvas.rect(0, height - 10 * mm, width, 10 * mm, fill=1, stroke=0)
        canvas.setFillColor(ORANGE)
        canvas.rect(0, height - 10 * mm, 24 * mm, 10 * mm, fill=1, stroke=0)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.setFillColor(colors.white)
        canvas.drawString(7 * mm, height - 6.5 * mm, "SOLARIS")
        canvas.setFont("Helvetica", 7.5)
        canvas.drawRightString(width - 12 * mm, height - 6.5 * mm, "API GUIDE - SPRING BOOT FOR DJANGO DEVELOPERS")
        canvas.setStrokeColor(LINE)
        canvas.line(17 * mm, 12 * mm, width - 17 * mm, 12 * mm)
        canvas.setFillColor(MUTED)
        canvas.setFont("Helvetica", 7.5)
        canvas.drawString(17 * mm, 8 * mm, "Solaris backend - source snapshot 2026-10-01")
        canvas.drawRightString(width - 17 * mm, 8 * mm, f"Page {doc.page}")
        canvas.restoreState()


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="CoverTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=28,
    leading=32, textColor=NAVY, alignment=TA_LEFT, spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="CoverSub", parent=styles["Normal"], fontName="Helvetica", fontSize=13,
    leading=18, textColor=BLUE, spaceAfter=16,
))
styles.add(ParagraphStyle(
    name="H1x", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=19,
    leading=23, textColor=NAVY, spaceBefore=8, spaceAfter=9,
))
styles.add(ParagraphStyle(
    name="H2x", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=13,
    leading=17, textColor=BLUE, spaceBefore=8, spaceAfter=6,
))
styles.add(ParagraphStyle(
    name="H3x", parent=styles["Heading3"], fontName="Helvetica-Bold", fontSize=10.5,
    leading=14, textColor=NAVY, spaceBefore=5, spaceAfter=3,
))
styles.add(ParagraphStyle(
    name="Bodyx", parent=styles["BodyText"], fontName="Helvetica", fontSize=9.2,
    leading=13.2, textColor=INK, spaceAfter=6,
))
styles.add(ParagraphStyle(
    name="Small", parent=styles["BodyText"], fontName="Helvetica", fontSize=7.8,
    leading=10.5, textColor=INK,
))
styles.add(ParagraphStyle(
    name="Tiny", parent=styles["BodyText"], fontName="Helvetica", fontSize=6.8,
    leading=9, textColor=INK,
))
styles.add(ParagraphStyle(
    name="CodeX", parent=styles["BodyText"], fontName="Courier", fontSize=7.2,
    leading=9.5, textColor=NAVY, backColor=PALE_BLUE, borderPadding=5, spaceAfter=6,
))
styles.add(ParagraphStyle(
    name="Callout", parent=styles["BodyText"], fontName="Helvetica", fontSize=9,
    leading=13, textColor=INK, backColor=PALE_ORANGE, borderColor=GOLD,
    borderWidth=0.6, borderPadding=8, spaceBefore=4, spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="Endpoint", parent=styles["Heading3"], fontName="Helvetica-Bold", fontSize=10.5,
    leading=14, textColor=colors.white, backColor=BLUE, borderPadding=6, spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="Ref", parent=styles["BodyText"], fontName="Courier", fontSize=6.6,
    leading=8.8, textColor=colors.HexColor("#36545F"),
))
styles.add(ParagraphStyle(
    name="Center", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=9,
    leading=12, alignment=TA_CENTER, textColor=NAVY,
))
styles.add(ParagraphStyle(
    name="TableHeader", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=7.8,
    leading=10.5, textColor=colors.white,
))


def p(text: str, style: str = "Bodyx") -> Paragraph:
    return Paragraph(text, styles[style])


def bullet(text: str) -> Paragraph:
    return Paragraph(f"<font color='#F29F05'>&#8226;</font> {text}", styles["Bodyx"])


def section(title: str, number: str | None = None) -> list:
    label = f"{number}  {title}" if number else title
    return [Spacer(1, 3 * mm), p(label, "H1x")]


def endpoint_block(endpoint: dict[str, str]):
    title = f"{endpoint['method']}  {endpoint['path']}"
    rows = [
        [p("Access", "Small"), p(endpoint["access"], "Small")],
        [p("Purpose", "Small"), p(endpoint["purpose"], "Small")],
        [p("Input", "Small"), p(endpoint["input"], "Small")],
        [p("Success", "Small"), p(endpoint["success"], "Small")],
        [p("Rules / edge behavior", "Small"), p(endpoint["rules"], "Small")],
        [p("Java implementation", "Small"), p(endpoint["refs"], "Ref")],
    ]
    table = Table(rows, colWidths=[34 * mm, 139 * mm], hAlign="LEFT")
    table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BACKGROUND", (0, 0), (0, -1), PALE),
        ("TEXTCOLOR", (0, 0), (0, -1), BLUE),
        ("FONTNAME", (0, 0), (0, -1), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return KeepTogether([p(escape(title), "Endpoint"), table, Spacer(1, 4 * mm)])


def code(text: str) -> Paragraph:
    return p(escape(text).replace("\n", "<br/>"), "CodeX")


ENDPOINT_GROUPS = [
    ("Authentication", [
        dict(method="POST", path="/api/auth/register", access="Public",
             purpose="Create a self-service homeowner account. This is the Spring equivalent of a DRF registration view or serializer create method.",
             input="JSON: name, email, password. Name is 2-120 chars; password is 8-72 chars; email must be valid.",
             success="201 Created with no response body.",
             rules="Email is normalized to lowercase; password is BCrypt-hashed; role is always HOMEOWNER; duplicate email is 409.",
             refs="controller/AuthController.java:20-27; service/AuthService.java:37-52; dto/auth/RegisterRequest.java:10-22; repository/UserRepository.java:16-18"),
        dict(method="POST", path="/api/auth/login", access="Public",
             purpose="Verify credentials and issue an access/refresh token pair.",
             input="JSON: email, password.", success="200 OK with accessToken, refreshToken, expiry seconds, user identity, and role.",
             rules="Disabled users and bad credentials both return 401 without revealing which check failed. The refresh token state is stored in Redis.",
             refs="controller/AuthController.java:29-36; service/AuthService.java:54-64,89-110; security/JwtService.java:37-59; security/RedisRefreshTokenStore.java:24-31"),
        dict(method="POST", path="/api/auth/refresh", access="Public endpoint, valid REFRESH token required in body",
             purpose="Rotate a refresh token and obtain a new token pair without resending the password.",
             input="JSON: refreshToken.", success="200 OK with a new access token and a new refresh token.",
             rules="Token must be signed, unexpired, type REFRESH, present in Redis, and tied to an existing enabled user. Redis atomically consumes it, so replay returns 401.",
             refs="controller/AuthController.java:38-42; service/AuthService.java:66-80,89-137; security/RedisRefreshTokenStore.java:17-20,33-45; dto/auth/RefreshTokenRequest.java:9-12"),
        dict(method="POST", path="/api/auth/logout", access="Public endpoint, valid active REFRESH token required in body",
             purpose="Revoke the browser or mobile client's refresh session.",
             input="JSON: refreshToken.", success="204 No Content.",
             rules="The Redis record is atomically consumed. A revoked, rotated, malformed, or already-used token returns 401. Existing short-lived access tokens expire naturally.",
             refs="controller/AuthController.java:44-48; service/AuthService.java:82-87,112-137; security/RedisRefreshTokenStore.java:33-45"),
    ]),
    ("Admin user management", [
        dict(method="GET", path="/api/admin/users", access="ADMIN bearer token",
             purpose="List accounts for administration without exposing password hashes.",
             input="Query: page (default 0), size (default 20, max 100).", success="200 OK with PageResponse<AdminUserResponse>.",
             rules="Negative page or size outside 1-100 returns 400. Results sort newest first.",
             refs="controller/AdminUserController.java:26-31; service/AdminUserService.java:26-30; dto/PageResponse.java:5-28"),
        dict(method="GET", path="/api/admin/users/{id}", access="ADMIN bearer token",
             purpose="Inspect a single account for support or administration.", input="Path: positive numeric user ID.",
             success="200 OK with user metadata.", rules="Missing user is 404; password never appears in the DTO.",
             refs="controller/AdminUserController.java:38-45; service/AdminUserService.java:32-36; dto/user/AdminUserResponse.java:9-27"),
        dict(method="POST", path="/api/admin/users", access="ADMIN bearer token",
             purpose="Create a controlled HOMEOWNER or ADMIN account.",
             input="JSON: name, email, password, role, optional phone, enabled.", success="201 Created with the safe user DTO.",
             rules="Role must be ADMIN or HOMEOWNER; email is unique; password is BCrypt-hashed. This is the controlled path for ADMIN creation.",
             refs="controller/AdminUserController.java:33-36; service/AdminUserService.java:38-52; dto/user/CreateUserRequest.java:13-34"),
        dict(method="PUT", path="/api/admin/users/{id}", access="ADMIN bearer token",
             purpose="Change identity, role, enabled status, phone, and optionally password.",
             input="JSON: name, email, role, enabled, optional phone; omit password or use null to preserve it.", success="200 OK with updated user.",
             rules="Duplicate email is 409. The last enabled ADMIN cannot be demoted or disabled. A supplied password must be 8-72 chars.",
             refs="controller/AdminUserController.java:47-55; service/AdminUserService.java:55-84,99-106; dto/user/UpdateUserRequest.java:13-34"),
        dict(method="DELETE", path="/api/admin/users/{id}", access="ADMIN bearer token",
             purpose="Remove an account when business dependencies permit it.", input="Path: user ID.", success="204 No Content.",
             rules="An admin cannot delete their own account. The final enabled ADMIN is protected. Foreign-key dependencies result in 409.",
             refs="controller/AdminUserController.java:57-65; service/AdminUserService.java:86-106; repository/UserRepository.java:21-23"),
    ]),
    ("Admin site management", [
        dict(method="GET", path="/api/admin/sites", access="ADMIN bearer token", purpose="List solar installations for operations staff.",
             input="Query: page, size (max 100).", success="200 OK paginated, sorted by name.", rules="Invalid pagination returns 400.",
             refs="controller/AdminSiteController.java:31-36; service/AdminSiteService.java:38-42; repository/SolarSiteRepository.java:18-19"),
        dict(method="GET", path="/api/admin/sites/{id}", access="ADMIN bearer token", purpose="Inspect one site and its owner metadata.",
             input="Path: site ID.", success="200 OK.", rules="Missing site returns 404.",
             refs="controller/AdminSiteController.java:38-41; service/AdminSiteService.java:44-46,113-116"),
        dict(method="POST", path="/api/admin/sites", access="ADMIN bearer token", purpose="Create the ownership boundary for devices, batteries, telemetry, and alerts.",
             input="JSON: ownerId, unique code, name, optional address, positive capacityKw, active.", success="201 Created.",
             rules="Owner must exist and have HOMEOWNER role. Codes are trimmed and uppercased. Duplicate code is 409.",
             refs="controller/AdminSiteController.java:43-46; service/AdminSiteService.java:49-65,104-111; dto/site/SiteRequest.java:14-35"),
        dict(method="PUT", path="/api/admin/sites/{id}", access="ADMIN bearer token", purpose="Maintain site metadata or transfer an unused site.",
             input="Same JSON shape as create.", success="200 OK.",
             rules="Ownership cannot change while devices, batteries, telemetry, storage history, or alerts depend on the site; returns 409.",
             refs="controller/AdminSiteController.java:48-51; service/AdminSiteService.java:67-85,96-102"),
        dict(method="DELETE", path="/api/admin/sites/{id}", access="ADMIN bearer token", purpose="Remove a site that has never accumulated operational or historical dependencies.",
             input="Path: site ID.", success="204 No Content.", rules="Protected history is never cascaded away; any dependency causes 409.",
             refs="controller/AdminSiteController.java:53-57; service/AdminSiteService.java:87-102"),
    ]),
    ("Admin device management", [
        dict(method="GET", path="/api/admin/devices", access="ADMIN bearer token", purpose="Search the device inventory.",
             input="Query: page, size, optional status, type, siteId.", success="200 OK paginated by identifier.",
             rules="Enum values are strict. Device types: SOLAR_INVERTER, SMART_METER, BATTERY_CONTROLLER, WEATHER_SENSOR.",
             refs="controller/AdminDeviceController.java:33-41; service/AdminDeviceService.java:35-49; repository/DeviceRepository.java:16-19"),
        dict(method="GET", path="/api/admin/devices/{id}", access="ADMIN bearer token", purpose="Inspect one device plus site and owner references.",
             input="Path: device ID.", success="200 OK.", rules="Missing device returns 404.",
             refs="controller/AdminDeviceController.java:43-46; service/AdminDeviceService.java:51-54,108-118"),
        dict(method="POST", path="/api/admin/devices", access="ADMIN bearer token", purpose="Register a physical or logical monitoring device.",
             input="JSON: identifier, name, type, status, siteId, optional manufacturer/model/firmwareVersion/lastSeenAt.", success="201 Created.",
             rules="Identifier is unique and normalized uppercase. Site must exist and belong to a HOMEOWNER.",
             refs="controller/AdminDeviceController.java:48-51; service/AdminDeviceService.java:56-64,91-105; dto/device/DeviceRequest.java:16-45"),
        dict(method="PUT", path="/api/admin/devices/{id}", access="ADMIN bearer token", purpose="Update lifecycle state and descriptive metadata.",
             input="Same JSON shape as create.", success="200 OK.",
             rules="A device cannot move to another site after telemetry or alerts reference it; this preserves historical meaning.",
             refs="controller/AdminDeviceController.java:53-56; service/AdminDeviceService.java:67-80,91-105"),
        dict(method="DELETE", path="/api/admin/devices/{id}", access="ADMIN bearer token", purpose="Remove an unused device record.",
             input="Path: device ID.", success="204 No Content.", rules="Telemetry or alert dependencies produce 409 instead of cascading deletes.",
             refs="controller/AdminDeviceController.java:58-62; service/AdminDeviceService.java:82-88"),
    ]),
    ("Admin battery management", [
        dict(method="GET", path="/api/admin/batteries", access="ADMIN bearer token", purpose="List storage assets and their current snapshots.",
             input="Query: page, size (max 100).", success="200 OK paginated.", rules="Invalid pagination returns 400.",
             refs="controller/AdminBatteryController.java:31-36; service/AdminBatteryService.java:32-36; repository/BatteryRepository.java:14-16"),
        dict(method="GET", path="/api/admin/batteries/{id}", access="ADMIN bearer token", purpose="Inspect one battery and its site/owner.",
             input="Path: battery ID.", success="200 OK.", rules="Missing battery returns 404.",
             refs="controller/AdminBatteryController.java:38-41; service/AdminBatteryService.java:38-41,97-107"),
        dict(method="POST", path="/api/admin/batteries", access="ADMIN bearer token", purpose="Register a battery and its current capacity/charge snapshot.",
             input="JSON: identifier, name, siteId, capacityKwh, status, currentChargePercent, currentStoredEnergyKwh, lastUpdatedAt.", success="201 Created.",
             rules="Identifier is unique. Charge is 0-100. Stored energy cannot exceed capacity. Site must be a homeowner site.",
             refs="controller/AdminBatteryController.java:43-46; service/AdminBatteryService.java:43-51,78-95; dto/storage/BatteryRequest.java:19-50"),
        dict(method="PUT", path="/api/admin/batteries/{id}", access="ADMIN bearer token", purpose="Update the current battery snapshot or metadata.",
             input="Same JSON shape as create.", success="200 OK.", rules="History or alerts prevent moving the battery to another site.",
             refs="controller/AdminBatteryController.java:48-51; service/AdminBatteryService.java:54-67,78-95"),
        dict(method="DELETE", path="/api/admin/batteries/{id}", access="ADMIN bearer token", purpose="Remove an unused battery record.",
             input="Path: battery ID.", success="204 No Content.", rules="Storage history or alerts produce 409; historical records are preserved.",
             refs="controller/AdminBatteryController.java:53-57; service/AdminBatteryService.java:69-75"),
    ]),
    ("Admin settings and alerts", [
        dict(method="GET", path="/api/admin/settings", access="ADMIN bearer token", purpose="Read the singleton rules that drive background alert generation.",
             input="No body.", success="200 OK with thresholds and feature flags.", rules="Creates defaults if the singleton row does not yet exist.",
             refs="controller/AdminSystemSettingController.java:20-23; service/SystemSettingService.java:18-21,49-67"),
        dict(method="PUT", path="/api/admin/settings", access="ADMIN bearer token", purpose="Configure production, consumption, battery, offline, and email alert behavior.",
             input="JSON with three thresholds, four alert toggles, offline threshold minutes, and emailNotificationsEnabled.", success="200 OK.",
             rules="Low battery is 0-100; thresholds are nonnegative; offline minutes are 1-10080. Settings are rules, not alert events.",
             refs="controller/AdminSystemSettingController.java:25-28; service/SystemSettingService.java:23-36; dto/setting/SystemSettingRequest.java:15-39"),
        dict(method="GET", path="/api/admin/alerts", access="ADMIN bearer token", purpose="Monitor persisted alert events across all homeowners.",
             input="Query: optional type, severity, read, userId; page, size.", success="200 OK paginated newest first.",
             rules="Strict enums and bounded pagination. Optional filters are combined with JPA Specifications.",
             refs="controller/AdminAlertController.java:24-33; service/AlertService.java:42-59; repository/AlertRepository.java:16-19"),
    ]),
    ("Homeowner profile and sites", [
        dict(method="GET", path="/api/homeowner/profile", access="HOMEOWNER bearer token", purpose="Return the authenticated homeowner's own identity record.",
             input="No body.", success="200 OK.", rules="User ID comes from AuthenticationPrincipal, never from a client-supplied path or query value.",
             refs="controller/HomeownerController.java:26-29; service/HomeownerService.java:25-28,57-65"),
        dict(method="PUT", path="/api/homeowner/profile", access="HOMEOWNER bearer token", purpose="Let a homeowner maintain name, email, and phone.",
             input="JSON: name, email, optional phone.", success="200 OK.",
             rules="Duplicate email is 409. Changing email invalidates existing JWT subjects, so the client must log in again.",
             refs="controller/HomeownerController.java:31-36; service/HomeownerService.java:30-41; dto/homeowner/UpdateProfileRequest.java:11-23"),
        dict(method="GET", path="/api/homeowner/sites", access="HOMEOWNER bearer token", purpose="List only the current homeowner's solar sites.",
             input="No body.", success="200 OK array; empty array is valid.", rules="Repository query includes ownerId; filtering is done in SQL, not in frontend code.",
             refs="controller/HomeownerController.java:38-41; service/HomeownerService.java:43-48; repository/SolarSiteRepository.java:24-25"),
        dict(method="GET", path="/api/homeowner/sites/{id}", access="HOMEOWNER bearer token", purpose="Read an owned site without IDOR exposure.",
             input="Path: site ID.", success="200 OK.", rules="Query matches both id and ownerId. A foreign site returns 404, hiding its existence.",
             refs="controller/HomeownerController.java:43-46; service/HomeownerService.java:50-55; repository/SolarSiteRepository.java:27-28"),
    ]),
    ("Homeowner energy and reporting", [
        dict(method="GET", path="/api/homeowner/energy/summary", access="HOMEOWNER bearer token", purpose="Aggregate production, consumption, grid import/export, and reading count.",
             input="Query: optional siteId, from, to (ISO-8601 instants). Defaults to the last 24 hours.", success="200 OK; zero totals are valid.",
             rules="from must precede to; max range 366 days; supplied site must be owned. Ownership is included in the aggregate query.",
             refs="controller/HomeownerEnergyController.java:28-35; service/HomeownerEnergyService.java:29-37,52-68; repository/EnergyReadingRepository.java:27-42"),
        dict(method="GET", path="/api/homeowner/energy/readings", access="HOMEOWNER bearer token", purpose="Retrieve raw domain readings for client-side charts or analysis.",
             input="Query: optional siteId/from/to; page (default 0); size (default 50, max 200).", success="200 OK paginated newest first.",
             rules="Ownership and time range are part of the repository query. No unbounded telemetry load occurs.",
             refs="controller/HomeownerEnergyController.java:37-46; service/HomeownerEnergyService.java:39-49; repository/EnergyReadingRepository.java:19-25"),
        dict(method="GET", path="/api/homeowner/reports/energy", access="HOMEOWNER bearer token", purpose="Return daily aggregates using meaningful parameterized JDBC.",
             input="Query: optional siteId, from, to (ISO dates). Defaults to last 30 UTC dates; max 366 days.", success="200 OK array grouped by date and site.",
             rules="Site ownership is checked before the query and enforced again in SQL using ownerId. SQL parameters are never concatenated from user input.",
             refs="controller/HomeownerReportController.java:26-33; service/EnergyReportService.java:25-51; repository/EnergyReportJdbcRepository.java:16-57"),
    ]),
    ("Homeowner storage and alerts", [
        dict(method="GET", path="/api/homeowner/storage/status", access="HOMEOWNER bearer token", purpose="Show current state for batteries attached to owned sites.",
             input="No body.", success="200 OK array; empty is valid.", rules="Repository derives site.owner.id in SQL and entity graph avoids N+1 lookups.",
             refs="controller/HomeownerStorageController.java:29-32; service/HomeownerStorageService.java:31-36; repository/BatteryRepository.java:24-25"),
        dict(method="GET", path="/api/homeowner/storage/history", access="HOMEOWNER bearer token", purpose="Retrieve time-series charge and stored-energy history.",
             input="Query: optional batteryId/from/to; page; size (max 200). Defaults to seven days.", success="200 OK paginated newest first.",
             rules="Max 366 days. A foreign battery returns 404. Owner predicate remains in the reading query.",
             refs="controller/HomeownerStorageController.java:34-43; service/HomeownerStorageService.java:38-62; repository/StorageReadingRepository.java:16-22"),
        dict(method="GET", path="/api/homeowner/alerts", access="HOMEOWNER bearer token", purpose="List persisted alert events belonging to the current homeowner.",
             input="Query: optional read; page; size (max 100).", success="200 OK paginated newest first.",
             rules="User ID always comes from the JWT principal; entity graph loads related references efficiently.",
             refs="controller/HomeownerAlertController.java:26-33; service/AlertService.java:22-29; repository/AlertRepository.java:27-31"),
        dict(method="PUT", path="/api/homeowner/alerts/{id}/read", access="HOMEOWNER bearer token", purpose="Acknowledge an owned alert.",
             input="Path: alert ID; no body.", success="200 OK with the alert's read flag true.",
             rules="Lookup matches id and userId. A foreign or missing alert returns 404. Repeating the request is idempotent in effect.",
             refs="controller/HomeownerAlertController.java:35-38; service/AlertService.java:31-40; repository/AlertRepository.java:33-34"),
    ]),
]


def build_story():
    report = json.loads(REPORT.read_text())
    summary = report["summary"]
    story = []

    story.extend([
        Spacer(1, 18 * mm),
        p("Solaris", "CoverTitle"),
        p("Backend API guide for Django developers moving to Java and Spring Boot", "CoverSub"),
        Spacer(1, 4 * mm),
    ])
    badges = Table([
        [p("38", "Center"), p("127 / 127", "Center"), p("0", "Center"), p("44 / 44", "Center")],
        [p("unique API routes", "Tiny"), p("live HTTP checks passed", "Tiny"), p("live 5xx responses", "Tiny"), p("automated tests passed", "Tiny")],
    ], colWidths=[43 * mm] * 4)
    badges.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), PALE_BLUE),
        ("BOX", (0, 0), (-1, -1), 0.8, BLUE),
        ("INNERGRID", (0, 0), (-1, -1), 0.35, LINE),
        ("TOPPADDING", (0, 0), (-1, 0), 9),
        ("BOTTOMPADDING", (0, 1), (-1, 1), 9),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
    ]))
    story.extend([
        badges, Spacer(1, 9 * mm),
        p("What this guide answers", "H2x"),
        bullet("What each endpoint does, who may call it, and why it exists."),
        bullet("How a request travels through Spring Security, MVC controllers, services, repositories, Hibernate/JPA, JDBC, PostgreSQL, and Redis."),
        bullet("Where each behavior is implemented, using repository-relative Java file paths and exact line ranges."),
        bullet("How Django and Django REST Framework concepts map to Spring Boot concepts without pretending the frameworks are identical."),
        Spacer(1, 3 * mm),
        p("Line numbers refer to the Java source snapshot tested on 2026-10-01. They are navigation aids and may move after later edits.", "Callout"),
        Spacer(1, 12 * mm),
        p("Solar Energy Monitoring System", "H2x"),
        p("Java 25 - Spring Boot 4.1 - Spring Security - JPA/Hibernate - PostgreSQL - Redis - JWT - Gradle", "Bodyx"),
        PageBreak(),
    ])

    story.extend(section("Verification status", "1"))
    story.append(p(
        f"The running application was tested over real HTTP on localhost against the Docker PostgreSQL and Redis services. "
        f"The recorded run contains <b>{summary['total']} checks</b>, <b>{summary['passed']} passes</b>, "
        f"<b>{summary['failed']} failures</b>, and <b>{summary['serverErrors']} 5xx responses</b>. "
        "Fixtures were uniquely named, settings were restored, and database plus Redis cleanup was verified.",
        "Bodyx",
    ))
    verification_rows = [
        [p("Area", "TableHeader"), p("What was proven live", "TableHeader")],
        [p("Authentication", "Small"), p("Register, login, expired/invalid JWT, access-vs-refresh separation, rotation, replay rejection, logout, disabled/deleted users.", "Small")],
        [p("Authorization", "Small"), p("Missing auth 401, wrong role 403, homeowner ownership isolation returning 404 for foreign resources.", "Small")],
        [p("Admin CRUD", "Small"), p("Users, sites, devices, batteries, settings, alerts, duplicates, invalid enums, missing IDs, dependency conflicts, safe deletes.", "Small")],
        [p("Homeowner data", "Small"), p("Profile, sites, energy, storage, alerts, JDBC report, empty windows, date limits, pagination limits.", "Small")],
        [p("Runtime", "Small"), p("Spring Boot startup, Flyway validation, PostgreSQL access, Redis access, clean server logs, clean Gradle test and build.", "Small")],
    ]
    verification = Table(verification_rows, colWidths=[34 * mm, 139 * mm], repeatRows=1)
    verification.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.extend([verification, Spacer(1, 4 * mm), p(
        "No finite test suite proves every imaginable future failure, but this run covers every current route and the security, validation, ownership, conflict, empty-result, and boundary behaviors that are most likely to cause production defects.",
        "Callout"), PageBreak()])

    story.extend(section("Django-to-Spring mental model", "2"))
    mapping = [
        ("Django / DRF", "Solaris Spring Boot", "What changes in your thinking"),
        ("urls.py", "@RequestMapping + @Get/Post/Put/DeleteMapping", "Routes live beside controller methods rather than in one URL table."),
        ("APIView / ViewSet", "@RestController", "Controller receives HTTP input and delegates; it should not own business rules."),
        ("Serializer", "Request/response DTO + Bean Validation", "DTOs define the wire contract; @Valid triggers validation before service execution."),
        ("Model", "@Entity", "JPA maps Java objects and relationships to relational rows."),
        ("Manager / QuerySet", "Repository", "Spring Data derives queries from method names; custom JPQL/JDBC is used when clearer."),
        ("Service module", "@Service", "Transactions and business rules live here, explicitly separated from controllers."),
        ("Middleware / authentication class", "Servlet Filter + SecurityFilterChain", "JWT authentication happens before MVC chooses a controller."),
        ("permission_classes", "Route authorization + ownership queries", "Roles gate route families; repository predicates enforce object ownership."),
        ("transaction.atomic", "@Transactional", "Spring opens, commits, or rolls back the transaction around the service method."),
        ("DRF exception handler", "@RestControllerAdvice", "Exceptions are translated into one JSON error shape."),
        ("Celery beat task", "@Scheduled Spring component", "A managed scheduler triggers monitoring without manual threads."),
        ("cache backend", "Redis StringRedisTemplate", "Redis stores revocable refresh-session state, not domain records."),
    ]
    table_data = [[p(x, "TableHeader" if index == 0 else "Small") for x in row]
                  for index, row in enumerate(mapping)]
    map_table = LongTable(table_data, colWidths=[42 * mm, 54 * mm, 77 * mm], repeatRows=1)
    map_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.extend([map_table, Spacer(1, 5 * mm), p(
        "Important difference: role checks and object ownership are separate concerns. hasRole('HOMEOWNER') answers 'what kind of user is this?'; findByIdAndOwnerId answers 'does this exact row belong to this user?'. Solaris applies both.",
        "Callout"), PageBreak()])

    story.extend(section("How one request travels", "3"))
    flow = [
        ("1", "Tomcat servlet container", "Accepts HTTP and creates HttpServletRequest / HttpServletResponse."),
        ("2", "Spring Security filter chain", "JwtAuthenticationFilter validates Bearer token, token type, expiry, and current database user state."),
        ("3", "Authorization", "SecurityConfig permits public auth routes and enforces ADMIN or HOMEOWNER on route families."),
        ("4", "DispatcherServlet", "Spring MVC matches method + path to a @RestController method and converts JSON into a DTO."),
        ("5", "Bean Validation", "@Valid and constraint annotations reject bad input before business logic."),
        ("6", "Service", "Business rules, ownership checks, and @Transactional boundaries execute."),
        ("7", "Repository", "JPA/Hibernate or parameterized JDBC talks to PostgreSQL."),
        ("8", "Response", "DTO becomes JSON; exceptions become ApiErrorResponse through GlobalExceptionHandler."),
    ]
    flow_rows = []
    for number, title, detail in flow:
        flow_rows.append([p(number, "Center"), p(f"<b>{title}</b><br/>{detail}", "Small")])
    flow_table = Table(flow_rows, colWidths=[14 * mm, 159 * mm])
    flow_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), ORANGE), ("TEXTCOLOR", (0, 0), (0, -1), NAVY),
        ("ROWBACKGROUNDS", (1, 0), (1, -1), [colors.white, PALE_BLUE]),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    story.extend([flow_table, Spacer(1, 5 * mm), p("Core navigation references", "H2x"),
                  code("config/SecurityConfig.java:31-90\nsecurity/JwtAuthenticationFilter.java:34-103\nexception/GlobalExceptionHandler.java:27-156"),
                  PageBreak()])

    story.extend(section("Authentication, JWT, and Redis", "4"))
    story.extend([
        p("Access token", "H2x"),
        p("Short-lived JWT used only in Authorization: Bearer. It contains subject email, role, userId, token type ACCESS, issued-at, and expiry. The filter verifies the signature and expiry, rejects any non-ACCESS token, then loads the current user from PostgreSQL. That database check immediately invalidates access for deleted, disabled, email-changed, or role-changed users."),
        p("Refresh token", "H2x"),
        p("Longer-lived JWT used only by refresh/logout. It contains a unique jti and type REFRESH. Signature alone is insufficient: Redis must also contain an active record for that jti."),
        p("Why Redis exists", "H2x"),
        p("Pure JWT refresh tokens cannot be revoked before expiry. Solaris stores one short record per refresh token: key solaris:refresh:&lt;jti&gt;, value userId:SHA256(token), with Redis TTL equal to remaining JWT lifetime. The raw token is never stored."),
    ])
    rotation = Table([
        [p("Login", "Small"), p("Issue access + refresh; hash refresh; store jti state in Redis with TTL.", "Small")],
        [p("Refresh", "Small"), p("Verify JWT type/signature/expiry; atomically compare-and-delete Redis state; verify live user; issue a new pair.", "Small")],
        [p("Replay", "Small"), p("Old jti no longer exists, so the second use returns 401.", "Small")],
        [p("Logout", "Small"), p("Atomically consumes the Redis record, revoking future refresh.", "Small")],
        [p("Redis unavailable", "Small"), p("Return 503 rather than silently accepting an unverifiable refresh token.", "Small")],
    ], colWidths=[30 * mm, 143 * mm])
    rotation.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), PALE_ORANGE), ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.extend([rotation, Spacer(1, 5 * mm), code(
        "security/JwtService.java:37-67\nsecurity/RedisRefreshTokenStore.java:13-53\nservice/AuthService.java:54-137\nsecurity/JwtAuthenticationFilter.java:34-103"
    ), PageBreak()])

    story.extend(section("Domain model and ownership", "5"))
    domain_rows = [
        ("User", "ADMIN or HOMEOWNER account", "Owns sites; receives alerts"),
        ("SolarSite", "Physical installation and ownership boundary", "belongs to User"),
        ("Device", "Inverter, meter, controller, or sensor", "belongs to SolarSite"),
        ("Battery", "Current storage asset snapshot", "belongs to SolarSite"),
        ("EnergyReading", "Production/consumption/import/export event", "belongs to Site + Device"),
        ("StorageReading", "Charge/stored-energy event", "belongs to Site + Battery"),
        ("Alert", "Persisted event", "belongs to User; optional Site/Device/Battery"),
        ("SystemSetting", "Singleton alert rule configuration", "not an alert event"),
    ]
    dtable = Table([[p("Entity", "TableHeader"), p("Purpose", "TableHeader"), p("Relationship", "TableHeader")]] +
                   [[p(a, "Small"), p(b, "Small"), p(c, "Small")] for a, b, c in domain_rows],
                   colWidths=[34 * mm, 78 * mm, 61 * mm], repeatRows=1)
    dtable.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]), ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5), ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.extend([dtable, Spacer(1, 5 * mm), p(
        "The most important security rule is that homeowner queries travel through the ownership chain in SQL: site.owner.id, battery.site.owner.id, reading.site.owner.id, or alert.user.id. Returning 404 for a foreign ID prevents an attacker from confirming that another user's resource exists.",
        "Callout"), PageBreak()])

    number = 6
    for group_name, endpoints in ENDPOINT_GROUPS:
        story.extend(section(group_name, str(number)))
        for endpoint in endpoints:
            story.append(endpoint_block(endpoint))
        story.append(PageBreak())
        number += 1

    story.extend(section("Errors, validation, and status codes", str(number)))
    number += 1
    story.extend([
        p("Every API error uses the same basic JSON envelope: timestamp, status, error, message, path, and optional fieldErrors. The advice layer maps known exceptions and hides internal stack traces."),
        code('{\n  "timestamp": "2026-10-01T14:36:22Z",\n  "status": 400,\n  "error": "Bad Request",\n  "message": "Validation failed",\n  "path": "/api/admin/users",\n  "fieldErrors": {"email": ["must be a well-formed email address"]}\n}'),
    ])
    status_rows = [
        ("400", "Malformed JSON, validation, invalid enum/query/path type, invalid date range"),
        ("401", "Missing/invalid/expired access token, invalid/revoked/replayed refresh token"),
        ("403", "Authenticated but wrong role, or prohibited admin self-delete"),
        ("404", "Missing resource or foreign homeowner-owned resource"),
        ("405", "HTTP method not supported"),
        ("409", "Duplicate unique value, protected dependency, final-admin rule, optimistic lock"),
        ("503", "Redis or authentication data temporarily unavailable"),
        ("500", "Only genuinely unexpected server failures; generic message returned, details logged"),
    ]
    stable = Table([[p("Status", "TableHeader"), p("Meaning in Solaris", "TableHeader")]] +
                   [[p(a, "Small"), p(b, "Small")] for a, b in status_rows], colWidths=[27 * mm, 146 * mm], repeatRows=1)
    stable.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]), ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.extend([stable, Spacer(1, 5 * mm), code("exception/GlobalExceptionHandler.java:27-156\nsecurity/SecurityErrorWriter.java:21-52"), PageBreak()])

    story.extend(section("Background monitoring and persisted alerts", str(number)))
    number += 1
    story.extend([
        p("SystemSetting rows configure rules; Alert rows record events. This is analogous to keeping Django settings/model configuration separate from a Notification or Alert model."),
        bullet("@Scheduled invokes monitoring at a configurable fixed delay; no uncontrolled loop or manually managed thread is used."),
        bullet("A pessimistic lock on the singleton settings row serializes evaluation across application instances."),
        bullet("Offline device, low battery, high consumption, and low production rules create persisted alerts."),
        bullet("Repository exists checks enforce a one-hour cooldown per user/type/resource to reduce duplicate alerts."),
        bullet("The scheduled wrapper catches and logs RuntimeException so one failed run does not terminate future scheduling."),
        code("service/AlertMonitoringJob.java:9-23\nservice/AlertMonitoringService.java:26-130\nrepository/SystemSettingRepository.java:10-12\nrepository/AlertRepository.java:36-43"),
        PageBreak(),
    ])

    story.extend(section("Why one report uses JDBC", str(number)))
    number += 1
    story.extend([
        p("Most CRUD uses JPA because entity relationships and transactional updates are clearer there. The daily report uses NamedParameterJdbcTemplate because it is an aggregation query whose SQL shape is itself the domain logic: group by date and site, sum four measures, and count readings."),
        p("Safety properties", "H2x"),
        bullet("Named parameters bind ownerId, siteId, and timestamps; client input is never concatenated into SQL."),
        bullet("The owner predicate is inside SQL, so even a coding mistake above the repository does not expose another owner's rows."),
        bullet("The service validates the site and limits the requested date range to 366 days."),
        bullet("The result maps directly into DailyEnergyReportResponse rather than pretending an aggregate row is a JPA entity."),
        code("service/EnergyReportService.java:25-51\nrepository/EnergyReportJdbcRepository.java:13-57"),
        PageBreak(),
    ])

    story.extend(section("Postman workflow", str(number)))
    number += 1
    story.extend([
        p("Import output/api/Solaris.postman_collection.json. It contains all 38 unique routes and 39 runnable requests because login appears once for ADMIN and once for HOMEOWNER."),
        p("Recommended order", "H2x"),
        bullet("Set baseUrl and the four credential variables. Use an existing controlled ADMIN; public registration cannot create one."),
        bullet("Run Admin Login and Homeowner Login. Their test scripts save access tokens, refresh tokens, and homeownerUserId."),
        bullet("Create a site, device, and battery. The collection saves each returned ID for later requests."),
        bullet("Run homeowner reads only after the admin has associated resources with that homeowner."),
        bullet("Refresh rotates and overwrites the homeowner token variables. Logout clears them."),
        bullet("Delete in child-to-parent order: device/battery, site, then user, and only when no telemetry/history dependencies exist."),
        p("Telemetry ingestion note", "H2x"),
        p("This backend exposes telemetry read APIs but no public/admin ingestion endpoint in the current route set. EnergyReading and StorageReading records are expected to arrive from a trusted integration or a future ingestion module. The live suite seeded those records directly in PostgreSQL only as disposable test fixtures."),
        PageBreak(),
    ])

    story.extend(section("Source map and maintenance notes", str(number)))
    source_rows = [
        ("HTTP controllers", "controller/*.java", "Route, HTTP status, DTO binding, principal extraction"),
        ("Request/response contracts", "dto/**/*.java", "JSON fields, validation, password exclusion"),
        ("Business logic", "service/*.java", "Transactions, ownership, lifecycle and conflict rules"),
        ("Persistence", "repository/*.java", "JPA derived queries, Specifications, entity graphs, JDBC report"),
        ("Database model", "entity/*.java", "Tables, foreign keys, indexes, uniqueness, timestamps, optimistic versions"),
        ("Security", "config/SecurityConfig.java; security/*.java", "Route roles, JWT filter, token generation, Redis token state"),
        ("Errors", "exception/*.java", "Consistent client-safe JSON errors"),
        ("Runtime config", "resources/application.yaml", "PostgreSQL, Redis, JWT TTL, Flyway, monitoring interval"),
        ("Automated tests", "src/test/java/com/solaris/backend/**", "44 service, controller/security, repository/JDBC assertions"),
        ("Live suite", "scripts/live_api_e2e.py", "127 real HTTP checks with isolated fixture cleanup"),
    ]
    srct = Table([[p("Concern", "TableHeader"), p("Location", "TableHeader"), p("Responsibility", "TableHeader")]] +
                 [[p(a, "Small"), p(b, "Ref"), p(c, "Small")] for a, b, c in source_rows],
                 colWidths=[37 * mm, 61 * mm, 75 * mm], repeatRows=1)
    srct.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]), ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5), ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.extend([srct, Spacer(1, 5 * mm), p(
        "When adding an endpoint, preserve this chain: DTO validation -> thin controller -> transactional service rule -> ownership-aware repository query -> DTO response -> positive and negative tests. If any layer is skipped, the API usually becomes harder to secure or maintain.",
        "Callout")])

    return story


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = SolarisDocTemplate(str(OUTPUT))
    document.build(build_story())
    print(OUTPUT)


if __name__ == "__main__":
    main()
