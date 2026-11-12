import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Token di test per l'ambiente di sviluppo locale (con ruolo BUYER)
  const devToken = localStorage.getItem('jwt_token') || 'YOUR_DEV_BUYER_JWT_TOKEN';

  if (devToken && req.method !== 'GET') {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${devToken}`
      }
    });
    return next(authReq);
  }

  return next(req);
};