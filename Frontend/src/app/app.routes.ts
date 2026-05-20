import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'characters',
    loadComponent: () => import('./features/characters/characters.component').then(m => m.CharactersComponent)
  },
  {
    path: 'battle',
    loadComponent: () => import('./features/battle/battle.component').then(m => m.BattleComponent)
  },
  {
    path: 'result',
    loadComponent: () => import('./features/result/result.component').then(m => m.ResultComponent),
    canDeactivate: [(component: any) => {
      if (component && component.canDeactivate) {
        return component.canDeactivate();
      }
      return true;
    }]
  },
  {
    path: 'stats',
    loadComponent: () => import('./features/stats/stats.component').then(m => m.StatsComponent)
  },
  {
    path: '',
    redirectTo: '/characters',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/characters'
  }
];
