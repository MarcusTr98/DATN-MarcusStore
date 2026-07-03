export default [
  {
    section: "Tổng quan",
    modules: [
      {
        key: "DASHBOARD",
        name: "Bảng điều khiển",
        icon: "bi-speedometer2"
      }
    ]
  },

  {
    section: "Sản phẩm & kho",
    modules: [
      {
        key: "CATEGORY",
        name: "Quản lý danh mục",
        icon: "bi-grid-fill"
      },
      {
        key: "PRODUCT_GROUP",
        name: "Sản phẩm",
        icon: "bi-phone-fill",
        subs: [
          { key: "PRODUCT", label: "Sản phẩm gốc" },
          { key: "ATTRIBUTE", label: "Thuộc tính" },
          { key: "ATTRIBUTE_VALUE", label: "Giá trị thuộc tính" },
          { key: "SKU", label: "Tạo SKU" }
        ]
      }
    ]
  },

  {
    section: "Kênh bán hàng",
    modules: [
      {
        key: "ORDER",
        name: "Quản lý đơn hàng",
        icon: "bi-receipt"
      },
      {
        key: "VOUCHER",
        name: "Quản lý Voucher",
        icon: "bi-ticket-perforated"
      },
      {
        key: "FLASHSALE",
        name: "Flash Sale",
        icon: "bi-lightning"
      },
      {
        key: "DONGTIEN",
        name: "Quản lý dòng tiền",
        icon: "bi-cash-stack"
      }
    ]
  },

  {
    section: "Nội dung",
    modules: [
      {
        key: "POST_GROUP",
        name: "Bài viết",
        icon: "bi-file-earmark-text",
        subs: [
          {
            key: "POST",
            label: "Bài viết"
          },
          {
            key: "POST_CATEGORY",
            label: "Danh mục bài viết"
          }
        ]
      },
      {
        key: "BANNER",
        name: "Quản lý Banner",
        icon: "bi-image"
      },
      {
        key: "CONTACT",
        name: "Quản lý liên hệ",
        icon: "bi-headset"
      }
    ]
  },

  {
    section: "Hệ thống",
    modules: [
      {
        key: "USER",
        name: "Quản lý tài khoản",
        icon: "bi-people"
      },
      {
        key: "SYSTEM",
        name: "Cấu hình chung",
        icon: "bi-gear"
      }
    ]
  }
]