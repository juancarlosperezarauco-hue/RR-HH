<div align="center">
   <h1>Sistema Financiero</h1>
</div>

## 🏗 Estructura del proyecto

Frontend `hrcore_mobile` en flutter <br>
Backend `hrcore-api` desarrollado con FastAPI <br>
Frontend `hrcore-web` en Angular.

```
hrcore-system/
├── hrcore_mobile/
|   ├── assets/               # Imagenes
|   ├── lib/                  # Codigo fuente
|   ├── pubspec.yaml          # Librerias y configuración
│   └── ...
│
├── hrcore-api/               # Backend (Spring boot)
│   ├── .devcontainer/         # Configuración para entorno de desarrollo
│   │   ├── Dockerfile
│   │   └── devcontainer.json
│   ├── app/                   # Código principal de la API
│   ├── requirements.txt       # Dependencias Python
│   ├── Dockerfile.dev         # Imagen para desarrollo
│   └── ...
│
├── hrcore-web/               # Frontend (Angular)
│   ├── .devcontainer/         # Configuración para entorno de desarrollo
│   ├── src/                   # Código fuente del frontend
│   ├── Dockerfile.dev         # Imagen para desarrollo
│   └── ...
│
├── .env                       # Variables de entorno (desarrollo)
├── .env.sample                # Ejemplo de variables de entorno
├── .env.prod                  # Variables de entorno para producción
├── docker-compose.yml         # Orquestación para desarrollo
└── README.md
```


### Servicios (Docker)

| Servicio | Tecnología  | Puerto | Descripción   |
| -------- | ----------- | ------ | ------------- |
| serv-web | Angular     | 4200   | Interfaz web  |
| serv-api | Spring Boot | 8080   | API REST      |
| serv-db  | PostgreSQL  | 5432   | Base de datos |

---

### 🧯 Comandos Docker
En la raiz jecutar
```bash
cp .env.sample .env

# modifica el .env.sample BASE_URL en flutter
cp .env hrcore_mobile/.env
```

```bash
# Iniciar servicios
docker compose up --build

# Iniciar en background
docker compose up --build -d

# Ver logs
docker compose logs -f

# Detener servicios
docker compose down

# Resetear base de datos
docker compose down -v

# Eliminar container, network, volumes sin uso
docker container prune
docker network prune
docker volume prune
```

## 🧯 Ejecutar DevContainer en VSCode

### ⚠️ instalar devcontainer en VSCODE

1. **En VSCode**  
   Abrir `hrcore-api` o `hrcore-web`.

2. **En otra ventana Ejecutar la base de datos (solo para hrcore-api)**

   ```bash
   docker compose up --build serv-hrcore-db
   ```

3. **Abrir el Command Palette**

- Presiona `Ctrl + Shift + P`.
- Busca y selecciona la opción `Dev Containers: Reopen in Container.`

5. **Ejecutar proyectos**

- En `hrcore-api`

  ```bash
  ./mvnw spring-boot:run
  ```

- En `hrcore-web`
  ```bash
  npm start
  ```
- Listo !!!


<div align="center" width="100">
  <h1>Stack</h1>
  <!-- Languages -->
  </br>
  <h3>Languages and Frameworks</h3>
	<img
     src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/java/java-original.svg"
     width="60px"
     alt="Java">
  <img
     src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original.svg"
     width="60px"
     alt="Spring Boot">
  <img
	  src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/angularjs/angularjs-original.svg"
	  width="60px"
	  alt="Angular">
  <img 
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/flutter/flutter-original.svg" 
    width="60px"
    alt="Flutter"/>   

  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/postgresql/postgresql-original-wordmark.svg"
    width="60px"
    alt="PostgreSQL">
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/html5/html5-original-wordmark.svg"
    width="60px"
    alt="HTML5">
    &nbsp;&nbsp;&nbsp;&nbsp;
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/css3/css3-original-wordmark.svg"
    width="60px"
    alt="css3">
    &nbsp;&nbsp;&nbsp;&nbsp;
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/tailwindcss/tailwindcss-original.svg"
    width="60px"
    alt="Tailwind CSS">
    <!-- Frameworks -->
	
  </br>
    <!-- tools -->
  </br>
  <h3>Tools</h3>
  <img
    src="https://cdn.simpleicons.org/jira/0052CC"
    width="55px"
    alt="Jira">
  <img
    src="https://cdn.simpleicons.org/github/FFFFFF"
    width="50px"
    alt="GitHub Logo White">
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/docker/docker-original-wordmark.svg"
    width="60px"
    alt="Docker">
    &nbsp;&nbsp;&nbsp;&nbsp;
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/git/git-original-wordmark.svg"
    width="65px"
    alt="Git Wordmark">
  <img
    src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/vscode/vscode-original-wordmark.svg"
    width="60px"
    alt="VS Code">
    &nbsp;&nbsp;&nbsp;&nbsp;
</div>
<hr>

last modified: 7/10/2026
