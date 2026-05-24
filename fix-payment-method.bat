@echo off
chcp 65001 >nul
set "FILE=D:\projects\nginx-1.18.0\html\bookstore\order-detail.html"

echo 正在修改支付方式为仅余额支付...

powershell -Command ^
"$content = Get-Content '%FILE%' -Encoding UTF8 -Raw; ^
$content = $content -replace '<div class=\"pay-options\" id=\"payOptions\">[^<]*<label><input type=\"radio\" name=\"pm\" value=\"BALANCE\" checked> 余额支付</label>[^<]*<label><input type=\"radio\" name=\"pm\" value=\"ALIPAY\"> 支付宝（模拟）</label>[^<]*<label><input type=\"radio\" name=\"pm\" value=\"WECHAT\"> 微信支付（模拟）</label>[^<]*</div>', '<div class=\"pay-options\" id=\"payOptions\"><label><input type=\"radio\" name=\"pm\" value=\"BALANCE\" checked> 余额支付</label></div>'; ^
Set-Content '%FILE%' -Value $content -Encoding UTF8 -NoNewline"

echo 修改完成！现在只支持余额支付。
pause
