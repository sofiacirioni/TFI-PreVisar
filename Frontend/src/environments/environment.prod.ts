//(Entorno de Producción):
export const environment = {
  production: true,
  // En producción, la llamada puede ser relativa ya que el frontend y el API
  // comparten el mismo dominio (a través de Nginx), o mantener el puerto si es explícito.
  // Para este caso, mantenemos el puerto absoluto para asegurar el ruteo.
  apiUrl: 'http://localhost:5712/api' 
};