import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Échec du démarrage : message lisible pour l'utilisateur, erreur relancée pour qu'elle reste visible en console.
bootstrapApplication(AppComponent, appConfig).catch((err: unknown) => {
  document.body.textContent = "L'application n'a pas pu démarrer. Rechargez la page.";
  throw err;
});
