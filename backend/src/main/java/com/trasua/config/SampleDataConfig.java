package com.trasua.config;

import com.trasua.domain.DrinkOrder;
import com.trasua.domain.Member;
import com.trasua.domain.OrderItem;
import com.trasua.repository.DrinkOrderRepository;
import com.trasua.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class SampleDataConfig {
    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner sampleTeaData(MemberRepository members, DrinkOrderRepository orders) {
        return args -> {
            if (members.count() > 0) {
                return;
            }
            Member minh = member("Minh Anh", "minh.anh@example.test", "MB Bank", "970422", "0869123456", "NGUYEN MINH ANH");
            Member linh = member("Linh Chi", "linh.chi@example.test", "Vietcombank", "970436", "1029384756", "TRAN LINH CHI");
            Member tuan = member("Tuấn", "tuan@example.test", "ACB", "970416", "2177999999", "LE ANH TUAN");
            Member huong = member("Hương", "huong@example.test", "Techcombank", "970407", "1903765432", "PHAM THU HUONG");
            Member nam = member("Nam", "nam@example.test", "BIDV", "970418", "9988123456", "DO QUOC NAM");
            members.saveAll(List.of(minh, linh, tuan, huong, nam));

            DrinkOrder order = new DrinkOrder();
            order.setOrderDate(LocalDate.now());
            order.setPayer(linh);
            order.setNote("Dữ liệu mẫu - sửa hoặc xoá trước khi dùng thật");
            OrderItem item1 = item(minh, "Trà đá", 5000, 1);
            OrderItem item2 = item(linh, "Trà tắc", 10000, 1);
            OrderItem item3 = item(tuan, "Cà phê sữa", 20000, 1);
            OrderItem item4 = item(huong, "Trà đá", 5000, 1);
            order.setTotalAmount(item1.getLineTotal() + item2.getLineTotal() + item3.getLineTotal() + item4.getLineTotal());
            order.replaceItems(List.of(item1, item2, item3, item4));
            orders.save(order);
        };
    }

    private Member member(String name, String email, String bank, String bin, String account, String accountName) {
        Member member = new Member();
        member.setDisplayName(name);
        member.setEmail(email);
        member.setBankName(bank);
        member.setBankBin(bin);
        member.setAccountNumber(account);
        member.setAccountName(accountName);
        return member;
    }

    private OrderItem item(Member consumer, String drink, long price, int quantity) {
        OrderItem item = new OrderItem();
        item.setConsumer(consumer);
        item.setDrinkName(drink);
        item.setUnitPrice(price);
        item.setQuantity(quantity);
        item.setLineTotal(price * quantity);
        return item;
    }
}
