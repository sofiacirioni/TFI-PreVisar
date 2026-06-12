import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { LUCIDE_ICONS } from './lucide-icons';

/** Wraps Lucide inner markup in a shared 24px stroke SVG. */
function wrap(inner: string): string {
  return (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" ' +
    'stroke="currentColor" stroke-width="1.8" stroke-linecap="round" ' +
    `stroke-linejoin="round">${inner}</svg>`
  );
}

/**
 * Registers every Lucide icon as an Angular Material SVG icon at app start,
 * so `<mat-icon svgIcon="name">` renders the design-system iconography.
 */
export function provideLucideIcons(): EnvironmentProviders {
  return provideAppInitializer(() => {
    const registry = inject(MatIconRegistry);
    const sanitizer = inject(DomSanitizer);
    for (const [name, inner] of Object.entries(LUCIDE_ICONS)) {
      registry.addSvgIconLiteral(name, sanitizer.bypassSecurityTrustHtml(wrap(inner)));
    }
  });
}
