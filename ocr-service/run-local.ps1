$ErrorActionPreference = 'Stop'

$serviceDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $serviceDir

function Test-PythonExecutable {
    param([string]$Path)

    if (-not $Path) {
        return $false
    }

    if ($Path -like '*\Microsoft\WindowsApps\*' -or $Path -like '*\MySQL Workbench*') {
        return $false
    }

    try {
        & $Path -c "import encodings, venv, sys; raise SystemExit(0 if sys.version_info >= (3, 10) and sys.version_info < (3, 13) else 1)" | Out-Null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Resolve-PythonExecutable {
    $candidates = @()

    foreach ($launcherArg in @('-3.12', '-3.11', '-3.10')) {
        try {
            py $launcherArg -c "import encodings, venv, sys; raise SystemExit(0 if sys.version_info >= (3, 10) and sys.version_info < (3, 13) else 1)" | Out-Null
            if ($LASTEXITCODE -eq 0) {
                return "py $launcherArg"
            }
        } catch {
        }
    }

    foreach ($commandName in @('python', 'python3')) {
        $command = Get-Command $commandName -ErrorAction SilentlyContinue
        if ($command) {
            $candidates += $command.Source
        }
    }

    $commonRoots = @("$env:LOCALAPPDATA\Programs\Python")

    foreach ($root in $commonRoots) {
        if (Test-Path -LiteralPath $root) {
            $candidates += Get-ChildItem -LiteralPath $root -Recurse -Filter python.exe -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty FullName
        }
    }

    if (Test-Path -LiteralPath 'C:\Program Files') {
        $pythonDirs = Get-ChildItem -LiteralPath 'C:\Program Files' -Directory -Filter 'Python*' -ErrorAction SilentlyContinue
        foreach ($pythonDir in $pythonDirs) {
            $candidates += Get-ChildItem -LiteralPath $pythonDir.FullName -Recurse -Filter python.exe -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty FullName
        }
    }

    foreach ($candidate in ($candidates | Where-Object { $_ } | Select-Object -Unique)) {
        if (Test-PythonExecutable $candidate) {
            return $candidate
        }
    }

    return $null
}

$python = Resolve-PythonExecutable
if (-not $python) {
    throw @'
Python 3.10, 3.11, or 3.12 64-bit executable was not found.

Install a compatible runtime with:
py install 3.12
'@
}

$venvPython = '.\.venv\Scripts\python.exe'

if ((Test-Path -LiteralPath '.venv') -and -not (Test-Path -LiteralPath $venvPython)) {
    throw @'
The existing .venv folder is incomplete.

Delete ocr-service\.venv and run this script again.
'@
}

if (-not (Test-Path -LiteralPath '.venv')) {
    if ($python.StartsWith('py ')) {
        $pythonArgs = $python.Split(' ')
        & $pythonArgs[0] $pythonArgs[1] -m venv .venv
    } else {
        & $python -m venv .venv
    }
}

if (-not (Test-Path -LiteralPath $venvPython)) {
    throw 'Failed to create the Python virtual environment.'
}

& $venvPython -m pip install --upgrade pip setuptools wheel
& $venvPython -m pip install -r requirements.txt
& $venvPython -m uvicorn app:app --host 127.0.0.1 --port 8000
