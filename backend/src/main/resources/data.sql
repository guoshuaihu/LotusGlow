INSERT INTO admin_user (id, username, password, display_name, role_name, created_at, updated_at) VALUES
  (1, 'admin', 'Admin@123', '珠宝运营管理员', 'SUPER_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO category (id, name, icon, sort_order) VALUES
  (1, '戒指', '◇', 1),
  (2, '项链', '◯', 2),
  (3, '耳饰', '✦', 3),
  (4, '手链', '◎', 4);

INSERT INTO banner (id, title, subtitle, image_url, link_type, link_value, sort_order) VALUES
  (1, '鎏光臻礼季', '高定珠宝专场，满额赠礼', 'https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?auto=format&fit=crop&w=1200&q=80', 'PRODUCT', '1', 1),
  (2, '纪念日心选', '甄选求婚与纪念日珠宝', 'https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=1200&q=80', 'CATEGORY', '1', 2);

INSERT INTO content_block (id, block_key, title, content) VALUES
  (1, 'brand_story', '品牌故事', '以现代工艺重塑东方珠宝灵感，每一件作品都附带材质说明与售后保障。'),
  (2, 'brand_service', '服务承诺', '支持 7 天无忧沟通、顺丰保价发货、专业清洁保养建议。'),
  (3, 'home_notice', '会员权益', '首单免运费，专属客服 1 对 1 跟进定制需求。');

INSERT INTO product (id, category_id, name, subtitle, product_no, base_price, sales_count, cover_image, description, certificate_info, service_info, support_custom, hot_flag, new_flag, tags_csv, status, created_at, updated_at) VALUES
  (1, 1, '星河密镶钻戒', '18K 金群镶工艺，适合求婚与纪念日', 'RING-001', 6999.00, 268, 'https://images.unsplash.com/photo-1602173574767-37ac01994b2a?auto=format&fit=crop&w=1200&q=80', '采用 18K 金与密镶钻石设计，强调光泽层次与佩戴舒适度。', 'NGTC 证书可追溯，主石等级清晰展示。', '顺丰保价发货，支持专业养护咨询。', TRUE, TRUE, TRUE, '求婚,热卖,礼盒', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 2, '月华珍珠吊坠', '淡水珍珠与流线金属结合', 'NECKLACE-001', 2999.00, 156, 'https://images.unsplash.com/photo-1617038220319-276d3cfab638?auto=format&fit=crop&w=1200&q=80', '适合通勤与宴会场景，视觉轻盈。', '珍珠光泽与圆度经过筛选。', '附送擦银布与保养卡。', FALSE, TRUE, FALSE, '轻奢,通勤', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 3, '晨星流苏耳坠', '轻盈垂坠设计，修饰脸型', 'EARRING-001', 1999.00, 97, 'https://images.unsplash.com/photo-1629224316810-9d8805b95e76?auto=format&fit=crop&w=1200&q=80', '适合派对与纪念日晚宴场景。', '金属材质均通过检测。', '支持搭配建议服务。', FALSE, FALSE, TRUE, '上新,派对', 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_sku (id, product_id, sku_code, material, ring_size, weight_desc, sale_price, stock, status, created_at, updated_at) VALUES
  (1, 1, 'RING-001-YG-12', '18K 黄金', '12', '约 3.5g', 6999.00, 8, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, 'RING-001-WG-13', '18K 白金', '13', '约 3.6g', 7299.00, 5, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 2, 'NECKLACE-001-W', '18K 白金', '均码', '约 4.2g', 2999.00, 12, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 3, 'EARRING-001-RG', '18K 玫瑰金', '均码', '约 2.4g', 1999.00, 16, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_media (id, product_id, media_type, media_url, sort_order) VALUES
  (1, 1, 'IMAGE', 'https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=1200&q=80', 1),
  (2, 1, 'IMAGE', 'https://images.unsplash.com/photo-1617038260849-7e4c0d3730cf?auto=format&fit=crop&w=1200&q=80', 2),
  (3, 2, 'IMAGE', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=1200&q=80', 1),
  (4, 3, 'IMAGE', 'https://images.unsplash.com/photo-1629224316810-9d8805b95e76?auto=format&fit=crop&w=1200&q=80', 1);
