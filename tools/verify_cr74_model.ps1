[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Path
)

$ErrorActionPreference = 'Stop'

$expectedBytes = 284692656L
$expectedSha256 = 'e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb'
$artifact = Get-Item -LiteralPath $Path -ErrorAction Stop

if ($artifact.PSIsContainer) {
    throw "Expected a model file, received directory: $Path"
}
if ($artifact.Length -ne $expectedBytes) {
    throw "Unexpected model size: $($artifact.Length) bytes; expected $expectedBytes bytes."
}

$actualSha256 = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
    throw "Unexpected SHA-256: $actualSha256; expected $expectedSha256."
}

[pscustomobject]@{
    Path = $artifact.FullName
    Bytes = $artifact.Length
    Sha256 = $actualSha256
    Status = 'verified'
}
