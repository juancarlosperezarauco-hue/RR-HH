# Directrices de Feature-Sliced Design (FSD) y Estándares de Frontend
## Arquitectura Escala Empresarial para `hrcore-web` (Angular 20)

---

### 1. Principio Fundamental de Feature-Sliced Design (FSD)

El código se organiza en **capas jerárquicas estrictas**. Una capa solo puede importar elementos de capas inferiores, **nunca** de capas superiores ni en el mismo nivel cruzado (salvo a través de sus puntos de entrada públicos `index.ts`):

```
app/
 ├── app.routes.ts          (Routing & App Root)
 ├── pages/                 (Capa 4: Vistas enrutadas compuestas)
 ├── widgets/               (Capa 3: Bloques visuales complejos autónomos)
 ├── features/              (Capa 2: Casos de uso de negocio y formularios de usuario)
 ├── entities/              (Capa 1: Modelos de dominio y servicios HttpClient API)
 └── shared/                (Capa 0: Utilidades, UI base, interceptors, layouts)
```

---

### 2. Reglas de Importación entre Capas

1. **`shared`:** No importa de ninguna otra capa del sistema. Contiene interceptores, servicios globales de toast, layouts base y guards.
2. **`entities`:** Solo importa de `shared`. Modela las entidades de negocio (`employee-file`, `shift`, `attendance`, `absence`) y sus clientes API.
3. **`features`:** Importa de `entities` y `shared`. Contiene la lógica de aplicación (`application/*.usecase.ts`) y los componentes de interacción del usuario.
4. **`widgets`:** Importa de `features`, `entities` y `shared`. Ensambla vistas complejas como el `sidebar` o cards compuestas.
5. **`pages`:** Importa de `widgets`, `features`, `entities` y `shared`. Son los componentes raíz que se enlazan en `app.routes.ts`.

---

### 3. Estándares Técnicos en Angular 20

* **Reactividad Nativa:** Se prioriza el uso de `signal()`, `computed()` y `effect()`.
* **Encapsulamiento de RxJS:** En la capa de presentación (`ui/`), se evitan suscripciones manuales (`.subscribe()`). Los casos de uso (`application/`) encapsulan las promesas o signals utilizando `firstValueFrom()`.
* **Standalone Components:** Todos los componentes son `standalone: true` con importaciones explícitas de `CommonModule`, `ReactiveFormsModule` y componentes de `shared/ui`.
* **Diseño con Tailwind CSS v4:** Uso riguroso de tokens semánticos definidos en el proyecto: `bg-card`, `bg-muted`, `border-border`, `text-primary`, `text-muted-foreground`, `text-destructive`.
