import fs from "node:fs";
import path from "node:path";

const output = path.join(process.cwd(), "docs", "marcus-usecase-tong.drawio");
const esc = (s) => String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
let seq = 1;
const nextId = (prefix) => `${prefix}${seq++}`;
const cells = [];
const actorIds = new Map();
const useCaseIds = new Map();

const groups = [
  {
    title: "TÀI KHOẢN VÀ TRẢI NGHIỆM KHÁCH HÀNG",
    items: [
      ["UC-01", "Đăng ký, đăng nhập và khôi phục mật khẩu", ["Guest", "Customer", "Staff", "Admin"]],
      ["UC-03", "Quản lý hồ sơ khách hàng", ["Customer"]],
      ["UC-04", "Quản lý sổ địa chỉ và bản đồ động", ["Customer"]],
      ["UC-08", "Xem cửa hàng, lọc và xem chi tiết sản phẩm", ["Guest", "Customer"]],
      ["UC-09", "So sánh sản phẩm", ["Guest", "Customer"]],
      ["UC-10", "Xem trang chủ, Header và Footer", ["Guest", "Customer"]],
      ["UC-21", "Hiển thị sản phẩm mới", ["Guest", "Customer"]],
      ["UC-36", "Quản lý sản phẩm yêu thích", ["Customer"]],
    ],
  },
  {
    title: "DANH MỤC VÀ QUẢN TRỊ NỀN",
    items: [
      ["UC-02", "Quản lý tài khoản và phân quyền động", ["Admin"]],
      ["UC-05", "Quản lý danh mục sản phẩm", ["Admin"]],
      ["UC-06", "Quản lý sản phẩm", ["Staff", "Admin"]],
      ["UC-07", "Quản lý thuộc tính, biến thể và SKU", ["Admin"]],
      ["UC-18", "Quản lý kho", ["Admin"]],
      ["UC-26", "Quản lý System Settings", ["Admin"]],
    ],
  },
  {
    title: "CHECKOUT, VẬN CHUYỂN VÀ ĐƠN HÀNG",
    items: [
      ["UC-11", "Quản lý giỏ hàng", ["Customer"]],
      ["UC-12", "Thực hiện Checkout", ["Customer"]],
      ["UC-13", "Giao hàng qua GHN", ["Customer", "Staff", "Admin"]],
      ["UC-14", "Nhận hàng tại cửa hàng", ["Customer", "Staff", "Admin"]],
      ["UC-15", "Thanh toán VNPay", ["Customer"]],
      ["UC-16", "Quản lý đơn hàng và chi tiết đơn hàng", ["Staff", "Admin"]],
      ["UC-17", "Hủy đơn, hoàn tài nguyên và refund VNPay", ["Customer", "Admin"]],
    ],
  },
  {
    title: "KHUYẾN MẠI VÀ TƯƠNG TÁC",
    items: [
      ["UC-19", "Quản lý voucher", ["Admin"]],
      ["UC-20", "Quản lý chiến dịch Flash Sale", ["Admin"]],
      ["UC-22", "Quản lý đánh giá và bình luận", ["Customer", "Staff", "Admin"]],
      ["UC-23", "Gửi email giao dịch và ưu đãi", ["Customer", "Admin"]],
      ["UC-24", "Live Chat giữa khách hàng và Admin", ["Customer", "Admin"]],
      ["UC-25", "Quản lý thông báo hệ thống", ["Customer", "Admin"]],
      ["UC-27", "Quản lý liên hệ", ["Guest", "Customer", "Admin"]],
    ],
  },
  {
    title: "BÁO CÁO, NỘI DUNG VÀ TRÍ TUỆ NHÂN TẠO",
    items: [
      ["UC-28", "Đối soát tài chính", ["Admin"]],
      ["UC-29", "Thu thập và phân tích hành vi khách hàng", ["Admin"]],
      ["UC-30", "AI tư vấn bán hàng", ["Customer"]],
      ["UC-31", "Xem Dashboard thống kê", ["Admin"]],
      ["UC-32", "AI phân tích kinh doanh", ["Admin"]],
      ["UC-33", "Quản lý Banner", ["Admin"]],
      ["UC-34", "Quản lý bài viết Blog", ["Admin"]],
      ["UC-35", "Xem lịch sử thao tác", ["Admin"]],
    ],
  },
];

const pageWidth = 3900;
const pageHeight = 1740;
const boundaryX = 260;
const boundaryY = 100;
const boundaryW = 3380;
const boundaryH = 1540;
const panelW = 620;
const panelGap = 45;
const panelX0 = 340;
const firstUseCaseY = 275;
const groupColors = ["#EAF3F8", "#FCE4D6", "#E2F0D9", "#FFF2CC", "#E4DFEC"];
// Đặt nhóm khách hàng ở trái, nghiệp vụ chung ở giữa và quản trị ở phải.
const visualColumnByGroup = [0, 4, 1, 2, 3];

const geo = (x, y, w, h, relative = false) => `<mxGeometry x="${x}" y="${y}" width="${w}" height="${h}"${relative ? ' relative="1"' : ""} as="geometry"/>`;
const cell = (attrs, geometry) => `<mxCell ${attrs}>${geometry}</mxCell>`;

cells.push(cell(`id="title" value="SƠ ĐỒ USE CASE TỔNG - HỆ THỐNG MARCUSSTORE" style="text;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=24;fontStyle=1;fontColor=#1F4E79;" vertex="1" parent="1"`, geo(650, 20, 2600, 50)));
cells.push(cell(`id="system" value="HỆ THỐNG MARCUSSTORE" style="rounded=0;whiteSpace=wrap;html=1;verticalAlign=top;align=center;spacingTop=10;fontFamily=Arial;fontSize=17;fontStyle=1;strokeWidth=3;strokeColor=#1F4E79;fillColor=#FFFFFF;" vertex="1" parent="1"`, geo(boundaryX, boundaryY, boundaryW, boundaryH)));

const actorPositions = {
  Guest: [45, 330, "Khách vãng lai\n(Guest)"],
  Customer: [45, 1010, "Khách hàng\n(Customer)"],
  Staff: [3715, 410, "Nhân viên\n(Staff)"],
  Admin: [3715, 1060, "Quản lý\n(Admin)"],
};
for (const [key, [x, y, label]] of Object.entries(actorPositions)) {
  const actorId = nextId("actor");
  actorIds.set(key, actorId);
  cells.push(cell(`id="${actorId}" value="${esc(label)}" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;fontFamily=Arial;fontSize=15;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fontColor=#1F1F1F;" vertex="1" parent="1"`, geo(x, y, 115, 150)));
}

// Generalization: tam giác rỗng luôn hướng về Actor cha.
const actorGeneralizations = [
  ["Customer", "Guest"],
  ["Admin", "Staff"],
];
for (const [child, parent] of actorGeneralizations) {
  cells.push(cell(`id="${nextId("actorGen")}" value="" style="edgeStyle=none;curved=0;rounded=0;html=1;strokeWidth=3;strokeColor=#1F4E79;dashed=0;endArrow=block;endFill=0;endSize=18;" edge="1" parent="1" source="${actorIds.get(child)}" target="${actorIds.get(parent)}"`, geo(0, 0, 0, 0, true)));
}

// Không lặp Association nếu Actor con đã kế thừa đầy đủ từ Actor cha.
function removeInheritedActorLinks(actors) {
  const result = [...actors];
  if (result.includes("Guest") && result.includes("Customer"))
    result.splice(result.indexOf("Customer"), 1);
  if (result.includes("Staff") && result.includes("Admin"))
    result.splice(result.indexOf("Admin"), 1);
  return result;
}

groups.forEach((group, groupIndex) => {
  const visualColumn = visualColumnByGroup[groupIndex];
  const panelX = panelX0 + visualColumn * (panelW + panelGap);
  group.items.forEach(([code, name, actors], itemIndex) => {
    const useCaseId = nextId("uc");
    useCaseIds.set(code, useCaseId);
    const y = firstUseCaseY + itemIndex * 145;
    cells.push(cell(`id="${useCaseId}" value="${esc(`${code}: ${name}`)}" style="ellipse;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=14;fontStyle=0;strokeWidth=2;strokeColor=#1F4E79;fillColor=${groupColors[groupIndex]};fontColor=#1F1F1F;" vertex="1" parent="1"`, geo(panelX + 55, y, 510, 82)));
    for (const actor of removeInheritedActorLinks(actors)) {
      cells.push(cell(`id="${nextId("assoc")}" value="" style="edgeStyle=none;curved=0;rounded=0;html=1;strokeWidth=2;strokeColor=#7F8FA4;dashed=0;endArrow=none;endFill=0;" edge="1" parent="1" source="${actorIds.get(actor)}" target="${useCaseId}"`, geo(0, 0, 0, 0, true)));
    }
  });
});

// Chú giải màu, không đóng khung các nhóm Use Case trên vùng vẽ.
groups.forEach((group, groupIndex) => {
  const legendX = 385 + groupIndex * 645;
  cells.push(cell(`id="${nextId("legend")}" value="${esc(group.title)}" style="rounded=1;arcSize=10;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=12;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=${groupColors[groupIndex]};fontColor=#1F1F1F;" vertex="1" parent="1"`, geo(legendX, 1550, 560, 52)));
});

const relationships = [
  ["UC-12", "UC-11", "include"],
  ["UC-12", "UC-04", "include"],
  ["UC-13", "UC-12", "extend"],
  ["UC-14", "UC-12", "extend"],
  ["UC-15", "UC-12", "extend"],
  ["UC-17", "UC-16", "extend"],
  ["UC-19", "UC-12", "extend"],
];
for (const [source, target, kind] of relationships) {
  cells.push(cell(`id="${nextId("rel")}" value="${kind === "include" ? "«include»" : "«extend»"}" style="edgeStyle=none;curved=0;rounded=0;html=1;strokeWidth=3;strokeColor=#1F4E79;dashed=1;dashPattern=10 6;endArrow=open;endFill=0;fontFamily=Arial;fontSize=14;fontStyle=1;fontColor=#000000;labelBackgroundColor=#FFFFFF;" edge="1" parent="1" source="${useCaseIds.get(source)}" target="${useCaseIds.get(target)}"`, geo(0, 0, 0, 0, true)));
}

const model = `<mxGraphModel dx="1900" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageWidth}" pageHeight="${pageHeight}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join("")}</root></mxGraphModel>`;
const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net" modified="2026-08-06T00:00:00.000Z" agent="Codex" version="26.0.0" type="device" compressed="false"><diagram id="usecase-overview" name="Use Case tổng">${model}</diagram></mxfile>\n`;
fs.writeFileSync(output, xml, "utf8");
console.log(`Created ${output}`);
console.log(`Actors: ${actorIds.size}; Use cases: ${useCaseIds.size}; Relationships: ${relationships.length}`);
