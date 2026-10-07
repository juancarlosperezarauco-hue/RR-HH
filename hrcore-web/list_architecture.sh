#!/bin/bash

# Colores para la consola
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
GRAY='\033[0;90m'
NC='\033[0m' # Sin color

clear
echo -e "${CYAN}======================================================================${NC}"
echo -e "${CYAN}   MAPEO DE RUTAS PLANAS (TRAZABILIDAD DE ARQUITECTURA)               ${NC}"
echo -e "${CYAN}======================================================================${NC}"

ROOT_DIR=$(pwd)

echo -e "${YELLOW}[+] Cargando reglas de archivos .ignore-files...${NC}"

# 1. Definir exclusiones base fijas
IGNORE_PATTERNS=(".git" "node_modules" "__pycache__" "venv" ".venv" "build" "dist" ".DS_Store" "list_flat_architecture.sh")

# 2. Buscar dinámicamente archivos '.ignore-files' y leer sus líneas
while IFS= read -r ignore_file; do
    while IFS= read -r line || [ -n "$line" ]; do
        # Limpiar espacios en blanco
        line=$(echo "$line" | xargs)
        
        # Ignorar líneas vacías y comentarios
        if [ -n "$line" ] && [[ ! "$line" =~ ^# ]]; then
            # Limpiar caracteres comodín comunes para el filtrado de bash
            clean_rule=$(echo "$line" | sed 's/\*//g' | sed 's/\///g')
            if [ -n "$clean_rule" ]; then
                IGNORE_PATTERNS+=("$clean_rule")
            fi
        fi
    done < "$ignore_file"
done < <(find . -name ".ignore-files" 2>/dev/null)

echo -e "${GREEN}[+] Escaneando estructura de archivos...${NC}"
echo ""

# 3. Función recursiva para listar archivos en formato plano
scan_files() {
    local current_dir="$1"
    
    # Listar todos los archivos y carpetas del directorio actual
    for item in "$current_dir"/*; do
        # Validar si el archivo/carpeta existe (evitar bugs con directorios vacíos)
        [ -e "$item" ] || continue
        
        local name=$(basename "$item")
        local skip=false
        
        # Verificar si coincide con alguna regla de exclusión
        for pattern in "${IGNORE_PATTERNS[@]}"; do
            if [[ "$name" == *"$pattern"* ]]; then
                skip=true
                break
            fi
        done
        
        if [ "$skip" = true ]; then
            continue
        fi
        
        if [ -d "$item" ]; then
            # Es un directorio, entramos recursivamente
            scan_files "$item"
        else
            # Es un archivo, calculamos la ruta relativa
            local relative_path="${item#$ROOT_DIR/}"
            
            # Resaltar componentes core de la arquitectura (C4 / Entry points)
            if [[ "$name" =~ ^main\.(dart|py|ts|js)$ ]] || [ "$name" = "manage.py" ] || [ "$name" = "models.py" ]; then
                echo -e "${YELLOW}$relative_path${NC}"
            else
                echo -e "${GRAY}$relative_path${NC}"
            fi
        fi
    done
}

# Ejecutar el escaneo desde la raíz
scan_files "$ROOT_DIR"

echo -e "\n${CYAN}======================================================================${NC}"