import fs from "node:fs";
import path from "node:path";

const output = path.join(
  process.cwd(),
  "docs",
  "marcus-docs",
  "marcus-quan-he-thuc-the-3.2.drawio",
);
const esc = (s = "") =>
  String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
const geo = (x, y, w, h, relative = false) =>
  `<mxGeometry x="${x}" y="${y}" width="${w}" height="${h}"${relative ? ' relative="1"' : ""} as="geometry"/>`;
const cell = (attrs, geometry = "") => `<mxCell ${attrs}>${geometry}</mxCell>`;

const pages = [
  {
    name: "3.2.1 Tài khoản và phân quyền",
    entities: {
      Role: [90, 170],
      User: [560, 170],
      Permission: [1030, 170],
      RolePermission: [300, 400],
      UserPermission: [820, 400],
      EmailOTP: [90, 650],
      PendingRegistration: [350, 650],
      UserAddress: [650, 650],
      AuditLog: [950, 650],
    },
    relations: [
      ["Role", "User", "1", "N", "vai trò chính"],
      ["Role", "RolePermission", "1", "N", ""],
      ["Permission", "RolePermission", "1", "N", ""],
      ["User", "UserPermission", "1", "N", ""],
      ["Permission", "UserPermission", "1", "N", ""],
      ["User", "EmailOTP", "1", "N", "tra cứu theo email", true],
      ["User", "PendingRegistration", "0..1", "N", "dữ liệu đăng ký tạm", true],
      ["User", "UserAddress", "1", "N", "sổ địa chỉ"],
      ["User", "AuditLog", "0..1", "N", "người thao tác"],
    ],
  },
  {
    name: "3.2.2 Sản phẩm, thuộc tính và tồn kho",
    entities: {
      Category: [70, 160],
      Product: [400, 160],
      ProductImage: [760, 80],
      ProductSku: [760, 250],
      Attribute: [70, 470],
      AttributeValue: [400, 470],
      SkuAttributeValue: [760, 470],
      SpecAttribute: [70, 700],
      ProductSpecValue: [400, 700],
      ProductItem: [1050, 250],
      OrderItem: [1050, 570],
    },
    relations: [
      ["Category", "Category", "0..1", "N", "cha - con"],
      ["Category", "Product", "1", "N", ""],
      ["Product", "ProductImage", "1", "N", ""],
      ["Product", "ProductSku", "1", "N", ""],
      ["Attribute", "AttributeValue", "1", "N", ""],
      ["ProductSku", "SkuAttributeValue", "1", "N", ""],
      ["AttributeValue", "SkuAttributeValue", "1", "N", ""],
      ["Category", "SpecAttribute", "1", "N", ""],
      ["Product", "ProductSpecValue", "1", "N", ""],
      ["SpecAttribute", "ProductSpecValue", "1", "N", ""],
      ["ProductSku", "ProductItem", "1", "N", "đơn vị vật lý"],
      ["OrderItem", "ProductItem", "1", "N", "đã bán"],
    ],
  },
  {
    name: "3.2.3 Giỏ hàng, đơn hàng và khuyến mại",
    entities: {
      User: [70, 150],
      Cart: [360, 150],
      CartItem: [650, 150],
      ProductSku: [980, 150],
      FlashSaleSlot: [70, 460],
      FlashSaleItem: [360, 460],
      Order: [650, 460],
      OrderItem: [980, 460],
      Voucher: [70, 730],
      UserVoucher: [360, 730],
    },
    relations: [
      ["User", "Cart", "1", "0..1", ""],
      ["Cart", "CartItem", "1", "N", ""],
      ["ProductSku", "CartItem", "1", "N", ""],
      ["FlashSaleSlot", "FlashSaleItem", "1", "N", ""],
      ["ProductSku", "FlashSaleItem", "1", "N", ""],
      ["FlashSaleSlot", "CartItem", "1", "N", "có thể áp dụng"],
      ["User", "Order", "1", "N", ""],
      ["Order", "OrderItem", "1", "N", ""],
      ["ProductSku", "OrderItem", "1", "N", ""],
      ["FlashSaleSlot", "OrderItem", "1", "N", "giá Flash Sale"],
      ["Voucher", "Order", "1", "N", "tối đa 1 voucher/đơn"],
      ["User", "UserVoucher", "1", "N", ""],
      ["Voucher", "UserVoucher", "1", "N", ""],
    ],
  },
  {
    name: "3.2.4 Thanh toán, vận chuyển và hoàn tiền",
    entities: {
      Order: [100, 190],
      OrderTransaction: [480, 100],
      OrderStatusHistory: [480, 280],
      RefundRequest: [850, 190],
      User: [1180, 190],
      ShippingConfig: [190, 590],
      UserAddress: [560, 590],
    },
    relations: [
      ["Order", "OrderTransaction", "1", "N", "giao dịch"],
      ["Order", "OrderStatusHistory", "1", "N", "lịch sử trạng thái"],
      ["Order", "RefundRequest", "1", "N", "yêu cầu hoàn tiền"],
      [
        "OrderTransaction",
        "RefundRequest",
        "1",
        "N",
        "giao dịch thanh toán gốc",
      ],
      ["RefundRequest", "OrderTransaction", "0..1", "0..1", "giao dịch refund"],
      ["User", "RefundRequest", "0..1", "N", "yêu cầu / duyệt / xác nhận"],
      ["ShippingConfig", "Order", "1", "N", "cấu hình GHN", true],
      ["UserAddress", "Order", "1", "N", "sao chép địa chỉ khi Checkout", true],
    ],
  },
  {
    name: "3.2.5 Tương tác, liên hệ và thông báo",
    entities: {
      User: [70, 160],
      Product: [70, 420],
      OrderItem: [70, 680],
      CommentEvaluation: [470, 420],
      ReviewReply: [850, 200],
      ReviewImage: [850, 420],
      Wishlist: [470, 680],
      UserNotification: [850, 680],
      AdminNotification: [1180, 300],
      ContactRequest: [1180, 570],
    },
    relations: [
      ["User", "CommentEvaluation", "1", "N", "người đánh giá"],
      ["Product", "CommentEvaluation", "1", "N", ""],
      ["OrderItem", "CommentEvaluation", "1", "N", "xác minh đã mua"],
      ["CommentEvaluation", "ReviewReply", "1", "0..1", "phản hồi"],
      ["CommentEvaluation", "ReviewImage", "1", "N", "ảnh đánh giá"],
      ["User", "Wishlist", "1", "N", ""],
      ["Product", "Wishlist", "1", "N", ""],
      ["User", "UserNotification", "1", "N", "chuông khách hàng"],
    ],
    notes: [
      [
        1180,
        760,
        "AdminNotification và ContactRequest độc lập, không có khóa ngoại User trong source hiện tại.",
      ],
    ],
  },
  {
    name: "3.2.6 Nội dung, cấu hình và phân tích AI",
    entities: {
      BannerPosition: [90, 180],
      Banner: [420, 180],
      PostCategory: [90, 470],
      Post: [420, 470],
      User: [760, 470],
      SystemSetting: [90, 740],
      AiProductClick: [760, 180],
      AiAnalyticsReport: [1080, 470],
      Product: [1080, 180],
      OrderData: [1080, 740],
    },
    labels: { OrderData: "Order / Transaction / Behavior Data" },
    relations: [
      ["BannerPosition", "Banner", "1", "N", ""],
      ["PostCategory", "Post", "1", "N", ""],
      ["User", "Post", "1", "N", "tác giả"],
      ["Product", "AiProductClick", "1", "N", "ngữ cảnh sản phẩm", true],
      ["User", "AiProductClick", "0..1", "N", "nhận diện/ngữ cảnh", true],
      ["OrderData", "AiAnalyticsReport", "N", "N", "nguồn tổng hợp", true],
    ],
    notes: [
      [
        70,
        850,
        "SystemSetting và AiAnalyticsReport được lưu độc lập, không khai báo khóa ngoại trực tiếp.",
      ],
    ],
  },
];

function makePage(page, pageIndex) {
  let seq = 1;
  const id = (prefix) => `p${pageIndex}_${prefix}${seq++}`;
  const cells = [];
  const entityIds = new Map();
  cells.push(
    cell(
      `id="${id("title")}" value="${esc(page.name)}" style="text;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=22;fontStyle=1;fontColor=#1F4E79;" vertex="1" parent="1"`,
      geo(200, 25, 1200, 50),
    ),
  );
  cells.push(
    cell(
      `id="${id("legend1")}" value="Đường liền: quan hệ trực tiếp / khóa ngoại" style="rounded=1;whiteSpace=wrap;html=1;fontFamily=Arial;fontSize=12;strokeColor=#1F4E79;fillColor=#EAF3F8;" vertex="1" parent="1"`,
      geo(230, 90, 330, 38),
    ),
  );
  cells.push(
    cell(
      `id="${id("legend2")}" value="Đường đứt: phụ thuộc logic, không có khóa ngoại trực tiếp" style="rounded=1;whiteSpace=wrap;html=1;fontFamily=Arial;fontSize=12;strokeColor=#7F6000;fillColor=#FFF2CC;" vertex="1" parent="1"`,
      geo(620, 90, 420, 38),
    ),
  );
  for (const [name, [x, y]] of Object.entries(page.entities)) {
    const eid = id("entity");
    entityIds.set(name, eid);
    const label = page.labels?.[name] || name;
    const junction = /Permission$|Value$|Wishlist$/.test(name);
    cells.push(
      cell(
        `id="${eid}" value="${esc(label)}" style="rounded=1;arcSize=8;whiteSpace=wrap;html=1;align=center;verticalAlign=middle;fontFamily=Arial;fontSize=15;fontStyle=1;strokeWidth=2;strokeColor=#1F4E79;fillColor=${junction ? "#E2F0D9" : "#F7FBFF"};fontColor=#1F1F1F;" vertex="1" parent="1"`,
        geo(x, y, 250, 72),
      ),
    );
  }
  for (const [
    source,
    target,
    sourceCard,
    targetCard,
    label = "",
    dashed = false,
  ] of page.relations) {
    const edgeId = id("edge");
    cells.push(
      cell(
        `id="${edgeId}" value="${esc(label)}" style="edgeStyle=none;curved=0;rounded=0;html=1;strokeWidth=2;strokeColor=${dashed ? "#7F6000" : "#1F4E79"};dashed=${dashed ? 1 : 0};dashPattern=8 5;endArrow=none;startArrow=none;fontFamily=Arial;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;" edge="1" parent="1" source="${entityIds.get(source)}" target="${entityIds.get(target)}"`,
        geo(0, 0, 0, 0, true),
      ),
    );
    cells.push(
      cell(
        `id="${id("card")}" value="${esc(sourceCard)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=13;fontStyle=1;labelBackgroundColor=#FFFFFF;" vertex="1" connectable="0" parent="${edgeId}"`,
        `<mxGeometry x="-0.82" relative="1" as="geometry"><mxPoint as="offset"/></mxGeometry>`,
      ),
    );
    cells.push(
      cell(
        `id="${id("card")}" value="${esc(targetCard)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontFamily=Arial;fontSize=13;fontStyle=1;labelBackgroundColor=#FFFFFF;" vertex="1" connectable="0" parent="${edgeId}"`,
        `<mxGeometry x="0.82" relative="1" as="geometry"><mxPoint as="offset"/></mxGeometry>`,
      ),
    );
  }
  for (const [x, y, text] of page.notes || []) {
    cells.push(
      cell(
        `id="${id("note")}" value="${esc(text)}" style="shape=note;whiteSpace=wrap;html=1;size=16;fontFamily=Arial;fontSize=12;strokeColor=#A67C00;fillColor=#FFF2CC;" vertex="1" parent="1"`,
        geo(x, y, 350, 80),
      ),
    );
  }
  return `<diagram id="section-${pageIndex}" name="${esc(page.name)}"><mxGraphModel dx="1600" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1600" pageHeight="1000" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${cells.join("")}</root></mxGraphModel></diagram>`;
}

const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net" modified="2026-08-06T00:00:00.000Z" agent="Codex" version="26.0.0" type="device" compressed="false">${pages.map((p, i) => makePage(p, i + 1)).join("")}</mxfile>\n`;
fs.writeFileSync(output, xml, "utf8");
console.log(`Created ${output}`);
console.log(`Pages: ${pages.length}`);

export { pages };
