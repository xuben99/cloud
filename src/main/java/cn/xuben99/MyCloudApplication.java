package cn.xuben99;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MyCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyCloudApplication.class, args);
    }

}
