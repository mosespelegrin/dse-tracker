import { AfterViewInit, Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { GOOGLE_CLIENT_ID } from './config';

declare const google: any;

// Wraps Google Identity Services' rendered button. The script (loaded in
// index.html) attaches a global `google` object asynchronously, so this
// polls briefly for it rather than assuming it's ready on init.
@Component({
  selector: 'app-google-signin-button',
  standalone: true,
  template: `
    @if (clientIdConfigured) {
      <div #btnContainer></div>
    } @else {
      <p class="google-disabled-note">Google Sign-In isn't configured on this server yet.</p>
    }
  `,
  styles: [
    `
      .google-disabled-note {
        font-size: 12px;
        color: var(--text-3);
        text-align: center;
      }
    `,
  ],
})
export class GoogleSigninButton implements AfterViewInit {
  @ViewChild('btnContainer') btnContainer?: ElementRef<HTMLDivElement>;
  @Output() credential = new EventEmitter<string>();

  readonly clientIdConfigured = !!GOOGLE_CLIENT_ID;

  ngAfterViewInit() {
    if (!this.clientIdConfigured) return;
    this.waitForGoogle();
  }

  private waitForGoogle(attemptsLeft = 20) {
    if (typeof google !== 'undefined' && google?.accounts?.id) {
      this.render();
    } else if (attemptsLeft > 0) {
      setTimeout(() => this.waitForGoogle(attemptsLeft - 1), 150);
    }
  }

  private render() {
    if (!this.btnContainer) return;
    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: (response: { credential: string }) => this.credential.emit(response.credential),
    });
    google.accounts.id.renderButton(this.btnContainer.nativeElement, {
      theme: 'outline',
      size: 'large',
      width: 320,
    });
  }
}
