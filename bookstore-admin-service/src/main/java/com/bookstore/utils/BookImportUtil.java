package com.bookstore.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BookImportUtil {

    // CSV 格式：书名,售价,原价,分类ID,作者,库存,销量,评分,状态,删除
    private static final Pattern BOOK_PATTERN = Pattern.compile("^(.+?),(\\d+\\.\\d+),(\\d+\\.\\d+),(\\d+),(.+?),(\\d+),(\\d+),(\\d+\\.\\d+),(\\d+),(\\d+)$");
    
    // 分类顺序映射（1-10）
    private static final List<String> CATEGORY_ORDER = Arrays.asList(
        "中国当代小说", "外国文学畅销", "历史", "心理", "少儿",
        "科普", "经管", "国学", "生活", "艺术"
    );
    
    // 常见书籍作者映射
    private static final Map<String, String> AUTHOR_MAP = new HashMap<>();
    
    static {
        // 中国当代小说
        AUTHOR_MAP.put("活着", "余华");
        AUTHOR_MAP.put("平凡的世界", "路遥");
        AUTHOR_MAP.put("三体", "刘慈欣");
        AUTHOR_MAP.put("三体Ⅱ：黑暗森林", "刘慈欣");
        AUTHOR_MAP.put("三体Ⅲ：死神永生", "刘慈欣");
        AUTHOR_MAP.put("人生海海", "麦家");
        AUTHOR_MAP.put("人世间", "梁晓声");
        AUTHOR_MAP.put("长安的荔枝", "马伯庸");
        AUTHOR_MAP.put("显微镜下的大明", "马伯庸");
        AUTHOR_MAP.put("风起陇西", "马伯庸");
        AUTHOR_MAP.put("两京十五日", "马伯庸");
        AUTHOR_MAP.put("古董局中局", "马伯庸");
        AUTHOR_MAP.put("生死疲劳", "莫言");
        AUTHOR_MAP.put("蛙", "莫言");
        AUTHOR_MAP.put("红高粱家族", "莫言");
        AUTHOR_MAP.put("白鹿原", "陈忠实");
        AUTHOR_MAP.put("围城", "钱钟书");
        AUTHOR_MAP.put("边城", "沈从文");
        AUTHOR_MAP.put("骆驼祥子", "老舍");
        AUTHOR_MAP.put("茶馆", "老舍");
        AUTHOR_MAP.put("许三观卖血记", "余华");
        AUTHOR_MAP.put("兄弟", "余华");
        AUTHOR_MAP.put("第七天", "余华");
        AUTHOR_MAP.put("一句顶一万句", "刘震云");
        AUTHOR_MAP.put("我不是潘金莲", "刘震云");
        AUTHOR_MAP.put("繁花", "金宇澄");
        AUTHOR_MAP.put("额尔古纳河右岸", "迟子建");
        AUTHOR_MAP.put("云边有个小卖部", "张嘉佳");
        AUTHOR_MAP.put("从你的全世界路过", "张嘉佳");
        AUTHOR_MAP.put("撒野", "巫哲");
        AUTHOR_MAP.put("盗墓笔记", "南派三叔");
        AUTHOR_MAP.put("鬼吹灯", "天下霸唱");
        AUTHOR_MAP.put("庆余年", "猫腻");
        AUTHOR_MAP.put("雪中悍刀行", "烽火戏诸侯");
        AUTHOR_MAP.put("将夜", "猫腻");
        AUTHOR_MAP.put("风声", "麦家");
        AUTHOR_MAP.put("暗算", "麦家");
        AUTHOR_MAP.put("解密", "麦家");
        AUTHOR_MAP.put("推拿", "毕飞宇");
        AUTHOR_MAP.put("废都", "贾平凹");
        AUTHOR_MAP.put("秦腔", "贾平凹");
        AUTHOR_MAP.put("檀香刑", "莫言");
        AUTHOR_MAP.put("丰乳肥臀", "莫言");
        AUTHOR_MAP.put("黄金时代", "王小波");
        AUTHOR_MAP.put("白银时代", "王小波");
        AUTHOR_MAP.put("青铜时代", "王小波");
        AUTHOR_MAP.put("棋王", "阿城");
        AUTHOR_MAP.put("树王", "阿城");
        AUTHOR_MAP.put("孩子王", "阿城");
        
        // 外国文学
        AUTHOR_MAP.put("百年孤独", "加西亚·马尔克斯");
        AUTHOR_MAP.put("白夜行", "东野圭吾");
        AUTHOR_MAP.put("解忧杂货店", "东野圭吾");
        AUTHOR_MAP.put("恶意", "东野圭吾");
        AUTHOR_MAP.put("嫌疑人X的献身", "东野圭吾");
        AUTHOR_MAP.put("放学后", "东野圭吾");
        AUTHOR_MAP.put("幻夜", "东野圭吾");
        AUTHOR_MAP.put("新参者", "东野圭吾");
        AUTHOR_MAP.put("红手指", "东野圭吾");
        AUTHOR_MAP.put("祈祷落幕时", "东野圭吾");
        AUTHOR_MAP.put("被嫌弃的松子的一生", "山田宗树");
        AUTHOR_MAP.put("月亮与六便士", "毛姆");
        AUTHOR_MAP.put("刀锋", "毛姆");
        AUTHOR_MAP.put("面纱", "毛姆");
        AUTHOR_MAP.put("人性的枷锁", "毛姆");
        AUTHOR_MAP.put("一九八四", "乔治·奥威尔");
        AUTHOR_MAP.put("动物农场", "乔治·奥威尔");
        AUTHOR_MAP.put("了不起的盖茨比", "菲茨杰拉德");
        AUTHOR_MAP.put("飘", "玛格丽特·米切尔");
        AUTHOR_MAP.put("简·爱", "夏洛蒂·勃朗特");
        AUTHOR_MAP.put("呼啸山庄", "艾米莉·勃朗特");
        AUTHOR_MAP.put("傲慢与偏见", "简·奥斯汀");
        AUTHOR_MAP.put("悲惨世界", "雨果");
        AUTHOR_MAP.put("巴黎圣母院", "雨果");
        AUTHOR_MAP.put("基督山伯爵", "大仲马");
        AUTHOR_MAP.put("红与黑", "司汤达");
        AUTHOR_MAP.put("局外人", "加缪");
        AUTHOR_MAP.put("鼠疫", "加缪");
        AUTHOR_MAP.put("霍乱时期的爱情", "加西亚·马尔克斯");
        AUTHOR_MAP.put("教父", "马里奥·普佐");
        AUTHOR_MAP.put("肖申克的救赎", "斯蒂芬·金");
        AUTHOR_MAP.put("熔炉", "孔枝泳");
        AUTHOR_MAP.put("82年生的金智英", "赵南柱");
        AUTHOR_MAP.put("克拉拉与太阳", "石黑一雄");
        
        // 心理学/自我成长
        AUTHOR_MAP.put("被讨厌的勇气", "岸见一郎");
        AUTHOR_MAP.put("你当像鸟飞往你的山", "塔拉·韦斯特弗");
        AUTHOR_MAP.put("蛤蟆先生去看心理医生", "罗伯特·戴博德");
        AUTHOR_MAP.put("原子习惯", "詹姆斯·克利尔");
        AUTHOR_MAP.put("深度工作", "卡尔·纽波特");
        AUTHOR_MAP.put("思考，快与慢", "丹尼尔·卡尼曼");
        AUTHOR_MAP.put("乌合之众", "古斯塔夫·勒庞");
        AUTHOR_MAP.put("自卑与超越", "阿尔弗雷德·阿德勒");
        AUTHOR_MAP.put("梦的解析", "弗洛伊德");
        AUTHOR_MAP.put("爱的艺术", "埃里希·弗洛姆");
        AUTHOR_MAP.put("活出生命的意义", "维克多·弗兰克尔");
        AUTHOR_MAP.put("少有人走的路", "M·斯科特·派克");
        AUTHOR_MAP.put("非暴力沟通", "马歇尔·卢森堡");
        
        // 历史
        AUTHOR_MAP.put("人类简史", "尤瓦尔·赫拉利");
        AUTHOR_MAP.put("万历十五年", "黄仁宇");
        AUTHOR_MAP.put("明朝那些事儿", "当年明月");
        
        // 经管
        AUTHOR_MAP.put("经济学原理", "N.格里高利·曼昆");
        AUTHOR_MAP.put("富爸爸穷爸爸", "罗伯特·清崎");
        AUTHOR_MAP.put("穷查理宝典", "查理·芒格");
        AUTHOR_MAP.put("娱乐至死", "尼尔·波兹曼");
        
        // 科普
        AUTHOR_MAP.put("时间简史", "史蒂芬·霍金");
        AUTHOR_MAP.put("从一到无穷大", "乔治·伽莫夫");
        AUTHOR_MAP.put("费曼物理学讲义", "理查德·费曼");
        
        // 其他
        AUTHOR_MAP.put("小王子", "安托万·德·圣埃克苏佩里");
        AUTHOR_MAP.put("浮生六记", "沈复");
        AUTHOR_MAP.put("秋园", "杨本芬");
        AUTHOR_MAP.put("夜晚的潜水艇", "陈春成");
    }

    public static void main(String[] args) {
        String filePath = "D:\\projects\\书本.txt";
        List<String> sqlStatements = parseAndGenerateSQL(filePath);
        
        try {
            String outputPath = "D:\\IDEA\\demo\\competition-code\\bookstore\\src\\main\\resources\\db\\migration\\V5__import_books_from_txt.sql";
            Files.write(Paths.get(outputPath), sqlStatements);
            log.info("SQL 文件已生成：{}", outputPath);
            log.info("共生成 {} 条图书记录", countBooks(sqlStatements.get(0)));
        } catch (IOException e) {
            log.error("写入文件失败", e);
        }
    }
    
    private static int countBooks(String sql) {
        return sql.split("\\),").length - 1;
    }

    public static List<String> parseAndGenerateSQL(String filePath) {
        List<String> sqlList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        
        // 先删除book表所有数据
        sb.append("-- 清空book表\n");
        sb.append("DELETE FROM `book`;\n\n");
        sb.append("-- 从书本.txt 导入的图书数据（每类前50本，共10类500本）\n");
        sb.append("INSERT INTO `book` (`isbn`, `title`, `author`, `category_id`, `price`, `original_price`, `stock`, `sales_count`, `rating`, `status`, `deleted`) VALUES\n");

        int isbnCounter = 1;
        boolean first = true;
        Map<Integer, Integer> categoryCount = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            categoryCount.put(i, 0);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过空行、注释行、标题行
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("-") || 
                    line.contains("成品") || line.contains("格式") || line.contains("下面")) {
                    continue;
                }

                Matcher matcher = BOOK_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String title = matcher.group(1).trim();
                    String priceStr = matcher.group(2).trim();
                    String originalPriceStr = matcher.group(3).trim();
                    String categoryIdStr = matcher.group(4).trim();
                    String author = matcher.group(5).trim();
                    String stockStr = matcher.group(6).trim();
                    String salesStr = matcher.group(7).trim();
                    String ratingStr = matcher.group(8).trim();
                    String statusStr = matcher.group(9).trim();
                    String deletedStr = matcher.group(10).trim();
                    
                    double price = Double.parseDouble(priceStr);
                    double originalPrice = Double.parseDouble(originalPriceStr);
                    int categoryId = Integer.parseInt(categoryIdStr);
                    int stock = Integer.parseInt(stockStr);
                    int sales = Integer.parseInt(salesStr);
                    double rating = Double.parseDouble(ratingStr);
                    int status = Integer.parseInt(statusStr);
                    int deleted = Integer.parseInt(deletedStr);
                    
                    // 检查该类是否已达到50本
                    if (categoryCount.get(categoryId) >= 50) {
                        continue;
                    }

                    String isbn = String.format("978%010d", isbnCounter++);

                    if (!first) {
                        sb.append(",\n");
                    }
                    sb.append(String.format("('%s', '%s', '%s', %d, %.2f, %.2f, %d, %d, %.1f, %d, %d)",
                            isbn, escapeSql(title), escapeSql(author), categoryId, 
                            price, originalPrice, stock, sales, rating, status, deleted));
                    first = false;
                    
                    // 更新计数
                    categoryCount.put(categoryId, categoryCount.get(categoryId) + 1);
                }
            }
            sb.append(";\n");
            sqlList.add(sb.toString());
            
        } catch (IOException e) {
            log.error("读取文件失败", e);
        }
        
        // 打印统计信息
        log.info("各类别图书数量统计：");
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > 0) {
                log.info("分类 {}: {} 本", entry.getKey(), entry.getValue());
                total += entry.getValue();
            }
        }
        log.info("总计: {} 本", total);

        return sqlList;
    }
    
    private static String escapeSql(String str) {
        return str.replace("'", "''");
    }
}
