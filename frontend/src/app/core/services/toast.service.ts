import { Service, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';
export interface Toast { id: number; message: string; kind: ToastKind }

@Service()
export class ToastService {
  readonly items = signal<Toast[]>([]);
  private nextId = 0;

  show(message: string, kind: ToastKind = 'info'): void {
    const id = ++this.nextId;
    this.items.update((items) => [...items, { id, message, kind }]);
    window.setTimeout(() => this.dismiss(id), 4200);
  }

  dismiss(id: number): void {
    this.items.update((items) => items.filter((toast) => toast.id !== id));
  }
}
