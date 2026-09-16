import { Directive, ElementRef, OnDestroy, OnInit, inject } from '@angular/core';

// Adds .reveal (hidden, offset) on init and .reveal-visible the first time
// the element scrolls into view — see the matching CSS in styles.css. Used
// for below-the-fold sections where a load-time animation would already be
// "finished" before the user ever scrolls to it.
@Directive({
  selector: '[appReveal]',
  standalone: true,
})
export class RevealOnScrollDirective implements OnInit, OnDestroy {
  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  ngOnInit() {
    const element = this.el.nativeElement;
    element.classList.add('reveal');

    if (typeof IntersectionObserver === 'undefined') {
      element.classList.add('reveal-visible');
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            element.classList.add('reveal-visible');
            this.observer?.unobserve(element);
          }
        }
      },
      { threshold: 0.15 },
    );
    this.observer.observe(element);
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}
