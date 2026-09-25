import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ErreurApi, Promotion } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, leur tableau et l'ouverture de session.
 * Les écrans de gestion (comptes, promotions, étudiants : #60–#62) ne sont pas livrés : c'est affiché.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterLink, ErreurComponent],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly promotions = signal<Promotion[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.promotions.set(p), error: (e: ErreurApi) => this.erreur.set(e) });
  }
}
