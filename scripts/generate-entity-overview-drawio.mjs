import fs from "node:fs";
import path from "node:path";
import { pages } from "./generate-entity-relations-drawio.mjs";

const output = path.join(process.cwd(), "docs", "marcus-docs", "marcus-so-do-tong-quan-quan-he-thuc-the.drawio");
const esc = (s = "") => String(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
const geo = (x, y, w, h, relative = false) => `<mxGeometry x="${x}" y="${y}" width="${w}" height="${h}"${relative ? ' relative="1"' : ""} as="geometry"/>`;
const cell = (attrs, geometry = "") => `<mxCell ${attrs}>${geometry}</mxCell>`;
let seq = 1;
const id = (prefix) => `${prefix}${seq++}`;

const groupNames = [
  "Tài khoản và phân quyền",
  "Sản phẩm, thuộc tính và tồn kho",
  "Giỏ hàng, đơn hàng và khuyến mại",
  "Thanh toán, vận chuyển và hoàn tiền",
  "Tương tác, liên hệ và thông báo",
  "Nội dung, cấu hình và phân tích AI",
];
const groupColors = ["#D9EAF7", "#E2F0D9", "#FCE4D6", "#EDE7F6", "#FFF2CC", "#DDEBF7"];
const displayNames = {
  RolePermission: "Role_Permissions",
  UserPermission: "User_Permissions",
  SkuAttributeValue: "Sku_Attribute_Values",
  ProductSpecValue: "Product_Spec_Values",
  UserVoucher: "User_Vouchers",
  EmailOTP: "EmailOtps",
  AiProductClick: "AI_Product_Clicks",
  AiAnalyticsReport: "AI_Analytics_Reports",
  AiUsageEvent: "AI_Usage_Events",
  OrderData: "Order / Transaction / Behavior Data",
};

const entityGroup = new Map();
const orderedByGroup = Array.from({ length: 6 }, () => []);
pages.forEach((page, groupIndex) => {
  for (const entity of Object.keys(page.entities)) {
    if (entity === "OrderData" || entityGroup.has(entity)) continue;
    entityGroup.set(entity, groupIndex);
    orderedByGroup[groupIndex].push(entity);
  }
});

entityGroup.set("AiUsageEvent", 5);
orderedByGroup[5].push("AiUsageEvent");

const relations = [];
const relationKeys = new Set();
function addRelation(source, target, sourceCard, targetCard, label = "", dashed = false) {
  if (source === "OrderData" || target === "OrderData") return;
  const key = `${source}|${target}|${label}`;
  if (relationKeys.has(key)) return;
  relationKeys.add(key);
  relations.push([source, target, sourceCard, targetCard, label, dashed]);
}
pages.forEach((page) => page.relations.forEach((r) => addRelation(...r)));

// Hiệu chỉnh theo khóa ngoại thực tế trong database/source hiện tại.
for (let i = relations.length - 1; i >= 0; i--) {
  const [s, t] = relations[i];
  if ((s === "Product" && t === "AiProductClick") || (s === "User" && t === "ContactRequest")) {
    relationKeys.delete(`${s}|${t}|${relations[i][4]}`);
    relations.splice(i, 1);
  }
}
addRelation("User", "ContactRequest", "0..1", "N", "người gửi", false);
addRelation("Product", "AiProductClick", "1", "N", "sản phẩm AI gợi ý", false);
addRelation("Product", "AiUsageEvent", "0..1", "N", "sự kiện sử dụng AI", false);
const cells = [];
const entityIds = new Map();
const pageWidth = 4100;
const pageHeight = 2550;
cells.push(cell(`id="${id("title")}" value="SƠ ĐỒ TỔNG QUAN QUAN HỆ THỰC THỂ - MARCUSSTORE" style="text;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=25;fontStyle=1;fontColor=#1F4E79;" vertex="1" parent="1"`, geo(700, 20, 2700, 55)));

groupNames.forEach((name, index) => {
  const x = 180 + index * 650;
  cells.push(cell(`id="${id("legend")}" value="${esc(name)}" style="rounded=1;arcSize=8;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=13;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=${groupColors[index]};" vertex="1" parent="1"`, geo(x, 95, 530, 50)));
});
cells.push(cell(`id="${id("legend")}" value="Đường liền: khóa ngoại / quan hệ trực tiếp     |     Đường đứt: phụ thuộc logic, không có khóa ngoại trực tiếp" style="rounded=1;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=13;fontStyle=1;strokeColor=#7F6000;fillColor=#FFFFFF;" vertex="1" parent="1"`, geo(1050, 160, 2000, 42)));

orderedByGroup.forEach((entities, groupIndex) => {
  const x = 230 + groupIndex * 650;
  entities.forEach((entity, rowIndex) => {
    const eid = id("entity");
    entityIds.set(entity, eid);
    const label = displayNames[entity] || entity;
    cells.push(cell(`id="${eid}" value="${esc(label)}" style="rounded=1;arcSize=8;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=14;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=${groupColors[groupIndex]};fontColor=#1F1F1F;" vertex="1" parent="1"`, geo(x, 250 + rowIndex * 155, 430, 66)));
  });
});

for (const [source, target, sourceCard, targetCard, label, dashed] of relations) {
  if (!entityIds.has(source) || !entityIds.has(target)) continue;
  const edgeId = id("edge");
  cells.push(cell(`id="${edgeId}" value="${esc(label)}" style="edgeStyle=none;curved=0;rounded=0;html=1;strokeWidth=2;strokeColor=${dashed ? "#9C6500" : "#5B6B7A"};dashed=${dashed ? 1 : 0};dashPattern=8 5;startArrow=none;endArrow=none;fontFamily=Arial;fontSize=11;fontStyle=1;labelBackgroundColor=#FFFFFF;" edge="1" parent="1" source="${entityIds.get(source)}" target="${entityIds.get(target)}"`, geo(0, 0, 0, 0, true)));
  cells.push(cell(`id="${id("card")}" value="${esc(sourceCard)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;" vertex="1" connectable="0" parent="${edgeId}"`, `<mxGeometry x="-0.84" relative="1" as="geometry"><mxPoint as="offset"/></mxGeometry>`));
  cells.push(cell(`id="${id("card")}" value="${esc(targetCard)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;" vertex="1" connectable="0" parent="${edgeId}"`, `<mxGeometry x="0.84" relative="1" as="geometry"><mxPoint as="offset"/></mxGeometry>`));
}

const independent = ["SystemSetting", "AdminNotification", "AiAnalyticsReport"];
cells.push(cell(`id="${id("note")}" value="Thực thể độc lập, không có khóa ngoại trực tiếp: ${esc(independent.map((e) => displayNames[e] || e).join(", "))}." style="shape=note;whiteSpace=wrap;html=1;size=16;fontFamily=Arial;fontSize=13;strokeColor=#A67C00;fillColor=#FFF2CC;" vertex="1" parent="1"`, geo(1450, 2410, 1200, 70)));

const model = `<mxGraphModel dx="2050" dy="1100" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${pageWidth}" pageHeight="${pageHeight}" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join("")}</root></mxGraphModel>`;
const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net" modified="2026-08-06T00:00:00.000Z" agent="Codex" version="26.0.0" type="device" compressed="false"><diagram id="entity-overview" name="Sơ đồ tổng quan quan hệ thực thể">${model}</diagram></mxfile>\n`;
fs.writeFileSync(output, xml, "utf8");
console.log(`Created ${output}`);
console.log(`Entities: ${entityIds.size}; Relations: ${relations.filter(([s, t]) => entityIds.has(s) && entityIds.has(t)).length}`);
