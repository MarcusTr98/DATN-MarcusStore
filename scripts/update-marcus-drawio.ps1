param(
    [string]$SourceDir = 'C:\Users\Admin\Downloads',
    [string]$OutputDir = (Join-Path $PSScriptRoot '..\docs\uml\updated')
)

$ErrorActionPreference = 'Stop'
$OutputDir = [System.IO.Path]::GetFullPath($OutputDir)
[System.IO.Directory]::CreateDirectory($OutputDir) | Out-Null

function Load-Drawio([string]$Path) {
    $document = New-Object System.Xml.XmlDocument
    $document.PreserveWhitespace = $true
    $document.Load($Path)
    return $document
}

function Find-Diagram([System.Xml.XmlDocument]$Document, [string]$Name) {
    return $Document.mxfile.diagram | Where-Object { $_.name -eq $Name } | Select-Object -First 1
}

function Mark-DiagramUpdated([System.Xml.XmlDocument]$Document, [string]$Name) {
    $diagram = Find-Diagram $Document $Name
    if (-not $diagram) { throw "Không tìm thấy page để đánh dấu: $Name" }
    $diagram.SetAttribute('name', "[MARCUS - ĐÃ CẬP NHẬT] $Name")
}

function Replace-CellValue(
    [System.Xml.XmlDocument]$Document,
    [string]$DiagramName,
    [string]$OldValue,
    [string]$NewValue
) {
    $diagram = Find-Diagram $Document $DiagramName
    if (-not $diagram) { throw "Không tìm thấy page: $DiagramName" }
    $cell = $diagram.mxGraphModel.root.mxCell | Where-Object { $_.value -eq $OldValue } | Select-Object -First 1
    if (-not $cell) { throw "Không tìm thấy '$OldValue' trong page '$DiagramName'" }
    $cell.SetAttribute('value', $NewValue)
}

function Add-Entity(
    [System.Xml.XmlDocument]$Document,
    [string]$DiagramName,
    [string]$Id,
    [string]$Value,
    [int]$X,
    [int]$Y,
    [int]$Width = 190
) {
    $diagram = Find-Diagram $Document $DiagramName
    $root = $diagram.mxGraphModel.root
    if ($root.mxCell | Where-Object { $_.id -eq $Id }) { return }
    $cell = $Document.CreateElement('mxCell')
    $cell.SetAttribute('id', $Id)
    $cell.SetAttribute('parent', '1')
    $cell.SetAttribute('value', $Value)
    $cell.SetAttribute('vertex', '1')
    $cell.SetAttribute('style', 'rounded=1;arcSize=8;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=15;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=#E8F4FF;fontColor=#1F1F1F;')
    $geometry = $Document.CreateElement('mxGeometry')
    $geometry.SetAttribute('x', [string]$X)
    $geometry.SetAttribute('y', [string]$Y)
    $geometry.SetAttribute('width', [string]$Width)
    $geometry.SetAttribute('height', '72')
    $geometry.SetAttribute('as', 'geometry')
    $cell.AppendChild($geometry) | Out-Null
    $root.AppendChild($cell) | Out-Null
}

function Add-Relation(
    [System.Xml.XmlDocument]$Document,
    [string]$DiagramName,
    [string]$Id,
    [string]$Source,
    [string]$Target,
    [string]$Value
) {
    $diagram = Find-Diagram $Document $DiagramName
    $root = $diagram.mxGraphModel.root
    if ($root.mxCell | Where-Object { $_.id -eq $Id }) { return }
    $cell = $Document.CreateElement('mxCell')
    $cell.SetAttribute('id', $Id)
    $cell.SetAttribute('parent', '1')
    $cell.SetAttribute('source', $Source)
    $cell.SetAttribute('target', $Target)
    $cell.SetAttribute('edge', '1')
    $cell.SetAttribute('value', $Value)
    $cell.SetAttribute('style', 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;fontFamily=Arial;fontSize=12;strokeWidth=2;endArrow=ERone;startArrow=ERone;')
    $geometry = $Document.CreateElement('mxGeometry')
    $geometry.SetAttribute('relative', '1')
    $geometry.SetAttribute('as', 'geometry')
    $cell.AppendChild($geometry) | Out-Null
    $root.AppendChild($cell) | Out-Null
}

function Set-EdgeRouting([System.Xml.XmlDocument]$Document, [string]$Mode) {
    foreach ($diagram in $Document.mxfile.diagram) {
        $isActivity = $diagram.name -match 'Activity'
        $route = if ($Mode -eq 'ActivityByName' -and $isActivity) { 'orthogonal' } else { 'straight' }
        foreach ($cell in ($diagram.mxGraphModel.root.mxCell | Where-Object { $_.edge -eq '1' })) {
            $style = [string]$cell.style
            $parts = @($style -split ';' | Where-Object {
                $_ -and
                $_ -notmatch '^edgeStyle=' -and
                $_ -notmatch '^orthogonalLoop=' -and
                $_ -notmatch '^jettySize=' -and
                $_ -notmatch '^rounded='
            })
            if ($route -eq 'orthogonal') {
                $parts = @('edgeStyle=orthogonalEdgeStyle', 'rounded=0', 'orthogonalLoop=1', 'jettySize=auto') + $parts
            } else {
                $parts = @('edgeStyle=none', 'rounded=0') + $parts
            }
            $cell.SetAttribute('style', (($parts -join ';') + ';'))
        }
    }
}

$activitySource = Join-Path $SourceDir 'usecase-activity.drawio'
$activityOutput = Join-Path $OutputDir 'usecase-activity-marcus-updated.drawio'
$activity = Load-Drawio $activitySource

Replace-CellValue $activity 'Activity UC-12.4: Kiểm tra và tạo đơn' 'Chặn gửi lặp' 'Chặn request lặp bằng khóa/idempotency'
Replace-CellValue $activity 'Activity UC-12.4: Kiểm tra và tạo đơn' 'Tạo đơn và chi tiết' 'Tạo Orders, Order Items và Shipping Details'
Replace-CellValue $activity 'Activity UC-13: Tạo vận đơn GHN' 'Lưu Tracking Code vào đơn hàng' 'Lưu Tracking Code và trạng thái vào Shipping Details'
Replace-CellValue $activity 'Activity UC-13: Đồng bộ trạng thái từ GHN' 'Cập nhật đơn và lịch sử trạng thái' 'Cập nhật đơn, Shipping Details và lịch sử trạng thái'
Replace-CellValue $activity 'Activity UC-13: Đồng bộ trạng thái từ GHN' 'Bỏ qua trạng thái không hợp lệ' 'Chặn trạng thái chạy lùi hoặc không hợp lệ'
Replace-CellValue $activity 'Activity UC-15: Tiếp nhận IPN từ VNPay' 'Khóa và kiểm tra giao dịch đã xử lý' 'Khóa giao dịch, kiểm tra idempotency và trạng thái đã xử lý'
Replace-CellValue $activity 'Activity UC-17: Hủy đơn và hoàn tài nguyên' 'Cập nhật trạng thái hủy và lịch sử' 'Lưu Order Cancellation, cập nhật trạng thái và lịch sử'
Replace-CellValue $activity 'Activity UC-17: Hủy đơn và hoàn tài nguyên' 'Ghi nhận kết quả refund' 'Ghi nhận refund: thành công, chờ retry hoặc cần đối soát'
Replace-CellValue $activity 'Activity UC-24: Trao đổi Live Chat' 'Tạo hoặc tiếp tục phòng chat trong bộ nhớ' 'Tạo/tiếp tục phòng chat trong bộ nhớ và lưu metadata ẩn danh'
Replace-CellValue $activity 'Activity UC-24: Trao đổi Live Chat' 'Xóa phiên hoặc tự hủy sau thời gian không hoạt động' 'Kết thúc phiên, cập nhật metric rồi xóa nội dung khỏi bộ nhớ'
Replace-CellValue $activity 'Activity UC-25: Nhận và đọc thông báo' 'Tạo thông báo cho đúng người nhận' 'Chuẩn hóa loại sự kiện và tạo thông báo đúng người nhận'
Replace-CellValue $activity 'Activity UC-28: Đối soát giao dịch' 'Cập nhật trạng thái đối soát' 'Lưu trạng thái, người và thời điểm đối soát'
Replace-CellValue $activity 'Activity UC-28: Đối soát giao dịch' 'Hiển thị báo cáo và danh sách giao dịch' 'Hiển thị báo cáo, giao dịch và cho phép xuất Excel đúng quyền'
Replace-CellValue $activity 'Activity UC-29: Thu thập và phân tích dữ liệu hành vi' 'Ghi nhận sự kiện được hỗ trợ và ngữ cảnh' 'Ghi event ẩn danh: AI hiển thị, click sản phẩm và chuyển đổi đơn'
Replace-CellValue $activity 'Activity UC-29: Thu thập và phân tích dữ liệu hành vi' 'Tổng hợp KPI, xu hướng, sản phẩm và lý do hủy' 'Tổng hợp KPI, funnel AI → click → đơn hàng và lý do hủy'
Replace-CellValue $activity 'Activity UC-30: AI tư vấn bán hàng' 'Kiểm tra nội dung và giới hạn yêu cầu' 'Kiểm tra nội dung và rate limit 60 event/phút/IP'
Replace-CellValue $activity 'Activity UC-30: AI tư vấn bán hàng' 'Kiểm tra, chuẩn hóa phản hồi và ghi nhận sử dụng' 'Chuẩn hóa câu trả lời ngắn, lọc dữ liệu nhạy cảm và ghi telemetry'
Replace-CellValue $activity 'Activity UC-32: AI phân tích kinh doanh' 'Kiểm tra kỳ phân tích và tần suất tạo báo cáo' 'Kiểm tra kỳ, quyền và data fingerprint của báo cáo cache'
Replace-CellValue $activity 'Activity UC-32: AI phân tích kinh doanh' 'Tổng hợp KPI, xu hướng, sản phẩm và hủy đơn' 'Tổng hợp KPI, bảo hành, tài chính và funnel AI → đơn hàng'
Replace-CellValue $activity 'Activity UC-32: AI phân tích kinh doanh' 'Lưu báo cáo AI theo kỳ' 'Lưu JSON, model, kỳ và data fingerprint'

@(
    'Activity UC-12.4: Kiểm tra và tạo đơn',
    'Activity UC-13: Tạo vận đơn GHN',
    'Activity UC-13: Đồng bộ trạng thái từ GHN',
    'Activity UC-15: Tiếp nhận IPN từ VNPay',
    'Activity UC-17: Hủy đơn và hoàn tài nguyên',
    'Activity UC-24: Trao đổi Live Chat',
    'Activity UC-25: Nhận và đọc thông báo',
    'Activity UC-28: Đối soát giao dịch',
    'Activity UC-29: Thu thập và phân tích dữ liệu hành vi',
    'Activity UC-30: AI tư vấn bán hàng',
    'Activity UC-32: AI phân tích kinh doanh'
) | ForEach-Object { Mark-DiagramUpdated $activity $_ }
Set-EdgeRouting $activity 'ActivityByName'
$activity.Save($activityOutput)

$relationSource = Join-Path $SourceDir 'marcus-usecase-tong-mqhthucthe.drawio'
$relationOutput = Join-Path $OutputDir 'marcus-usecase-tong-mqhthucthe-updated.drawio'
$relations = Load-Drawio $relationSource

Add-Entity $relations '3.2.4 Thanh toán, vận chuyển và hoàn tiền' 'marcus_order_shipping_details' 'OrderShippingDetail' 400 430 190
Add-Entity $relations '3.2.4 Thanh toán, vận chuyển và hoàn tiền' 'marcus_order_cancellations' 'OrderCancellation' 650 430 190
Add-Relation $relations '3.2.4 Thanh toán, vận chuyển và hoàn tiền' 'marcus_rel_order_shipping' 'p4_entity4' 'marcus_order_shipping_details' '1 — 1 snapshot giao nhận'
Add-Relation $relations '3.2.4 Thanh toán, vận chuyển và hoàn tiền' 'marcus_rel_order_cancel' 'p4_entity4' 'marcus_order_cancellations' '1 — 0..1 sự kiện hủy'

Add-Entity $relations '3.2.5 Tương tác, liên hệ và thông báo' 'marcus_chat_session_metrics' 'ChatSessionMetric' 560 560 210
Add-Relation $relations '3.2.5 Tương tác, liên hệ và thông báo' 'marcus_rel_chat_metric' 'marcus_chat_session_metrics' 'p5_entity11' 'metadata ẩn danh; không lưu nội dung chat'

Replace-CellValue $relations '3.2.6 Nội dung, cấu hình và phân tích AI' 'AiAnalyticsReport' 'AiAnalyticsReport (data fingerprint)'
Replace-CellValue $relations '3.2.6 Nội dung, cấu hình và phân tích AI' 'Order / Transaction / Behavior Data' 'Order / Transaction / Warranty / AI Funnel Data'

@(
    '3.2.4 Thanh toán, vận chuyển và hoàn tiền',
    '3.2.5 Tương tác, liên hệ và thông báo',
    '3.2.6 Nội dung, cấu hình và phân tích AI'
) | ForEach-Object { Mark-DiagramUpdated $relations $_ }
Mark-DiagramUpdated $relations 'Trang-2'
Set-EdgeRouting $relations 'AllStraight'
$relations.Save($relationOutput)

Write-Output $activityOutput
Write-Output $relationOutput
