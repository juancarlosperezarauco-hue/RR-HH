# =========================================================================================
# Script: List-FlatArchitecture.ps1
# Objetivo: Listar rutas planas omitiendo archivos según los .ignore-files encontrados
# =========================================================================================

Clear-Host
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   MAPEO DE RUTAS PLANAS (TRAZABILIDAD DE ARQUITECTURA)               " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$RootDir = Get-Location

# 1. Recopilar todas las reglas de los .ignore-files del proyecto
Write-Host "[+] Cargando reglas de archivos .ignore-files..." -ForegroundColor Yellow
$GlobalIgnoreList = @(
    # Reglas base por si no hay gitignore en alguna carpeta
    ".git", "node_modules", "__pycache__", "venv", ".venv", "build", "dist", ".DS_Store"
)

$GitIgnores = Get-ChildItem -Filter ".ignore-files" -Recurse -ErrorAction SilentlyContinue
foreach ($IgnoreFile in $GitIgnores) {
    $Lines = Get-Content $IgnoreFile.FullName
    foreach ($Line in $Lines) {
        $Line = $Line.Trim()
        # Ignorar líneas vacías o comentarios
        if ($Line -and -not $Line.StartsWith("#")) {
            # Limpiar formatos comunes de gitignore para compatibilidad con PowerShell
            $CleanRule = $Line.Replace("*", "").Replace("/", "")
            if ($CleanRule -and $GlobalIgnoreList -notcontains $CleanRule) {
                $GlobalIgnoreList += $CleanRule
            }
        }
    }
}

Write-Host "[+] Escaneando estructura de archivos..." -ForegroundColor Green
Write-Host "" # <--- Corregido aquí

# 2. Función recursiva para listar en formato plano
function Get-FlatFiles {
    param ([string]$Path)
    
    $Items = Get-ChildItem -Path $Path
    
    foreach ($Item in $Items) {
        # Verificar si el archivo o carpeta coincide con alguna regla de exclusión
        $Skip = $false
        foreach ($Ignore in $GlobalIgnoreList) {
            if ($Item.Name -like "*$Ignore*") {
                $Skip = $true
                break
            }
        }
        
        if ($Skip) { continue }

        if ($Item.PSIsContainer) {
            # Es una carpeta, explorar recursivamente
            Get-FlatFiles -Path $Item.FullName
        } else {
            # Es un archivo, calcular la ruta relativa desde la raíz del proyecto
            $RelativePath = $Item.FullName.Replace($RootDir.Path + "\", "").Replace("\", "/")
            
            # Resaltar componentes core de la arquitectura en la consola
            if ($Item.Name -match "main\.(dart|py|ts|js)$" -or $Item.Name -eq "manage.py" -or $Item.Name -eq "models.py") {
                Write-Host $RelativePath -ForegroundColor DarkYellow
            } else {
                Write-Host $RelativePath -ForegroundColor Gray
            }
        }
    }
}

# Ejecutar el escaneo plano
Get-FlatFiles -Path $RootDir

Write-Host "`n======================================================================" -ForegroundColor Cyan