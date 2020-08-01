Param(
    [Parameter(Mandatory=$True)]
    [string]$mcvpath
)

# Return True if we can make firewall rule changes, False otherwise.
function Test-Admin
{
    $user = [Security.Principal.WindowsIdentity]::GetCurrent();
    (New-Object Security.Principal.WindowsPrincipal $user).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}

# Return True if there are any firewall rules for mcservl; False otherwise.
function Check-Mcservl-Firewall-Rule
{
    @(Get-NetFirewallApplicationFilter -Program "*mcservl.exe" | Get-NetFirewallRule).Count -gt 0
}

# Return True if there are any firewall rules for the McV status port, otherwise returns False.
function Check-Status-Port-Firewall-Rule
{
    @(Get-NetFirewallPortFilter | Where { $_.LocalPort -eq 8788  } | Get-NetFirewallRule).Count -gt 0
}

# not sure why "if (Test-Admin -and Check-Mcservl-Firewall-Rule)" would be True even when
# Check-Mcservl-Firewall-Rule would return False. at any rate, the following appears to
# work.
$is_admin = Test-Admin
$mcservl_firewalled = Check-Mcservl-Firewall-Rule
$mcv_firewalled = Check-Status-Port-Firewall-Rule

# only attempt to add our firewall rule if the current user is an admin
# and there are no existing rules for mcservl.
#if ($is_admin -and (-not $mcservl_firewalled)) {
if ($is_admin) {
    #$mcvpath = $mcvpath.TrimEnd("\")
    #$mcvpath = (Resolve-Path $mcvpath).ToString().TrimEnd("\")
    # C:\mcidasv\JAVA11~1\adde\bin\mcservl.exe
    $mcvpath = (Convert-Path $mcvpath).TrimEnd("\")
    if (-not $mcservl_firewalled) {
        New-NetFirewallRule `
            -DisplayName "McIDAS-V Local ADDE" `
            -Group "McIDAS-V" `
            -Profile Any `
            -Direction Inbound `
            -Action Allow `
            -EdgeTraversalPolicy Block `
            -Protocol TCP `
            -LocalPort 8112-8142 `
            -RemoteAddress 127.0.0.1 `
            -Program "$mcvpath\adde\bin\mcservl.exe"
    }

    if (-not $mcv_firewalled) {
        New-NetFirewallRule `
            -DisplayName "McIDAS-V Status Monitor" `
            -Group "McIDAS-V" `
            -Profile Any `
            -Direction Inbound `
            -Action Allow `
            -EdgeTraversalPolicy Block `
            -Protocol TCP `
            -LocalPort 8788 `
            -RemoteAddress 127.0.0.1 `
            -Program "$mcvpath\jre\bin\java.exe"
    }
}

Write-Host "is_admin: $is_admin, firewalled: $mcservl_firewalled"