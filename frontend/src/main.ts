import { bootstrapApplication } from '@angular/platform-browser';
import { Chart, registerables } from 'chart.js';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

// Chart.js v4 exige registro explicito dos controllers/scales usados.
// Registramos todos os built-in aqui pra evitar "X is not a registered controller"
// em qualquer componente que use ng2-charts.
Chart.register(...registerables);

bootstrapApplication(AppComponent, appConfig)
  .catch(err => console.error(err));
