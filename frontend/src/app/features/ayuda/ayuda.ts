import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';

interface FaqItem {
  pregunta: string;
  respuesta: string;
}

interface FaqCategoria {
  titulo: string;
  icono: string;
  items: FaqItem[];
}

/**
 * Página de ayuda / Preguntas Frecuentes (FAQ).
 *
 * Vista privada (bajo MainLayout): es material de soporte para el profesional
 * autenticado, accesible desde el menú de usuario. Contenido estático.
 */
@Component({
  selector: 'app-ayuda',
  imports: [RouterLink, MatExpansionModule, MatIconModule],
  templateUrl: './ayuda.html',
  styleUrl: './ayuda.scss',
})
export class Ayuda {
  readonly categorias: FaqCategoria[] = [
    {
      titulo: 'Sobre la aplicación',
      icono: 'help_outline',
      items: [
        {
          pregunta: '¿Qué es PreVisar?',
          respuesta:
            'Es una herramienta que te ayuda a armar y pre-validar expedientes técnicos antes de presentarlos ante el CIEC. Te guía en la carga de datos, genera documentos administrativos pre-completados y revisa el expediente para detectar problemas comunes antes de la presentación.',
        },
        {
          pregunta: '¿PreVisar presenta o aprueba mi expediente?',
          respuesta:
            'No. PreVisar es una capa de asistencia y pre-validación: te ayuda a que el expediente llegue en mejores condiciones. La presentación oficial, el visado y la aprobación se hacen en los sistemas del CIEC (miCIEC), como siempre. PreVisar no reemplaza ese circuito.',
        },
        {
          pregunta: 'Si la app no marca ningún problema, ¿mi expediente está aprobado?',
          respuesta:
            'No necesariamente. Las validaciones son informativas: reducen las causas más comunes de rechazo, pero no garantizan la aprobación. La revisión final siempre la hace el CIEC.',
        },
      ],
    },
    {
      titulo: 'Armado del expediente',
      icono: 'description',
      items: [
        {
          pregunta: '¿Tengo que subir los documentos en un orden determinado?',
          respuesta:
            'No. Podés cargarlos en el orden que quieras y a medida que los vayas consiguiendo. Esto es útil, por ejemplo, cuando esperás comprobantes de pago que gestiona el comitente.',
        },
        {
          pregunta: '¿Qué pasa si me falta un documento obligatorio?',
          respuesta:
            'La app te avisa, pero no te bloquea. Podés seguir trabajando y completar lo que falte más tarde. Las observaciones son avisos, no impedimentos.',
        },
        {
          pregunta: '¿Puedo subir varios archivos en una misma sección?',
          respuesta:
            'Depende de la sección. Algunas admiten varios archivos (por ejemplo, comprobantes) y otras esperan uno solo. La app te lo indica en cada caso.',
        },
        {
          pregunta: 'La app generó el contrato y la carátula. ¿Ya están listos?',
          respuesta:
            'Esos documentos se generan pre-completados para ayudarte, pero tenés que descargarlos, firmarlos y volver a subirlos. La versión firmada es la que cuenta para el expediente.',
        },
      ],
    },
    {
      titulo: 'Validaciones',
      icono: 'fact_check',
      items: [
        {
          pregunta: '¿Qué revisa la app en mis documentos?',
          respuesta:
            'Tres tipos de control: que los archivos tengan el formato correcto (tipo, tamaño, orientación), que los datos sean coherentes entre documentos (por ejemplo, montos y CUIT), y —con inteligencia artificial— la calidad visual de las páginas (legibilidad, firmas, sellos, escaneos cortados o de baja calidad).',
        },
        {
          pregunta: '¿Por qué una observación aparece como "IA visual"?',
          respuesta:
            'Porque la detectó el análisis con inteligencia artificial, que evalúa cómo se ve el documento. Es una sugerencia asistiva: te conviene revisarla, pero no es un veredicto definitivo.',
        },
        {
          pregunta: 'El análisis con IA tarda o no responde. ¿Qué hago?',
          respuesta:
            'El análisis usa un servicio externo que ocasionalmente puede estar sobrecargado. Si no responde, la app te avisa con el motivo; podés volver a intentarlo en unos minutos.',
        },
      ],
    },
    {
      titulo: 'Pagos',
      icono: 'payments',
      items: [
        {
          pregunta: '¿Qué puedo pagar desde la app?',
          respuesta: 'El arancel del CIEC correspondiente al expediente, a través de Mercado Pago.',
        },
        {
          pregunta: '¿Puedo hacer que pague el comitente?',
          respuesta:
            'Sí. Podés pagar vos directamente o generar un enlace de pago y compartirlo (por correo, WhatsApp o copiándolo) para que lo abone otra persona, sin que necesite tener cuenta en la aplicación.',
        },
        {
          pregunta: '¿PreVisar guarda los datos de mi tarjeta?',
          respuesta:
            'No. El pago lo procesa Mercado Pago; la app nunca almacena datos de tarjetas ni credenciales de pago, solo el resultado de la operación.',
        },
        {
          pregunta: 'Pagué pero el expediente sigue figurando como impago. ¿Por qué?',
          respuesta:
            'La confirmación del pago puede tardar unos instantes. El estado se actualiza automáticamente al acreditarse; si no, podés volver a entrar al expediente en unos minutos.',
        },
      ],
    },
    {
      titulo: 'Cuenta y datos',
      icono: 'account_circle',
      items: [
        {
          pregunta: '¿Cómo modifico mis datos?',
          respuesta:
            'Desde tu perfil podés acceder y actualizar tus datos en cualquier momento.',
        },
        {
          pregunta: '¿Cómo doy de baja mi cuenta?',
          respuesta:
            'Desde tu perfil podés solicitar la baja. Para la eliminación definitiva de tus datos personales, podés escribir al correo de contacto indicado en la Política de Privacidad.',
        },
        {
          pregunta: '¿Quién puede ver mis expedientes?',
          respuesta:
            'Solo vos. Cada profesional accede únicamente a sus propios expedientes; la información está aislada entre cuentas.',
        },
      ],
    },
  ];
}
