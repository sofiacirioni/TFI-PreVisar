import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EstadoPagoRetorno } from '../../../core/models/pago.model';

/**
 * Vista de retorno del checkout de Mercado Pago (SCRUM-182).
 *
 * IMPORTANTE: el retorno del navegador NO es la fuente de verdad del pago. Los
 * query params (status, payment_id, external_reference) son solo feedback visual;
 * el usuario puede cerrar la pestaña o manipular la URL. La acreditación real y el
 * registro del Pago llegan por el webhook server-to-server (SCRUM-187).
 * Por eso acá NO se persiste nada ni se cambia el estado del expediente.
 */
@Component({
  selector: 'app-pago-retorno',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './pago-retorno.html',
  styleUrl: './pago-retorno.scss',
})
export class PagoRetorno {
  private readonly route = inject(ActivatedRoute);

  // /pago/:estado → exito | pendiente | error (deriva de las back_urls de MP).
  readonly estado = (this.route.snapshot.paramMap.get('estado') ?? 'error') as EstadoPagoRetorno;

  // Datos informativos que MP agrega a la URL de retorno.
  private readonly qp = this.route.snapshot.queryParamMap;
  readonly status = this.qp.get('status') ?? this.qp.get('collection_status');
  readonly paymentId = this.qp.get('payment_id');
  readonly expedienteId = this.qp.get('external_reference');

  readonly info = computed(() => VISTA[this.estado] ?? VISTA['error']);

  // Volver al armado del expediente si sabemos cuál es; si no, al listado.
  readonly volverLink = computed(() =>
    this.expedienteId ? ['/expedientes', this.expedienteId, 'armado'] : ['/expedientes'],
  );
}

interface VistaPago {
  icono: string;
  clase: string;
  titulo: string;
  mensaje: string;
}

const VISTA: Record<EstadoPagoRetorno, VistaPago> = {
  exito: {
    icono: 'check_circle',
    clase: 'pago--exito',
    titulo: 'Pago aprobado',
    mensaje:
      'Recibimos la confirmación del pago. La acreditación definitiva se procesa en segundo plano.',
  },
  pendiente: {
    icono: 'schedule',
    clase: 'pago--pendiente',
    titulo: 'Pago pendiente',
    mensaje: 'El pago quedó pendiente de acreditación. Te avisaremos cuando se confirme.',
  },
  error: {
    icono: 'cancel',
    clase: 'pago--error',
    titulo: 'Pago rechazado',
    mensaje: 'No se pudo completar el pago. Podés volver al expediente e intentarlo nuevamente.',
  },
};
