package ru.bell.auto_form;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import ru.bell.auto_form.service.DiskService;
import ru.bell.auto_form.service.FormService;

import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class AutoFormApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(AutoFormApplication.class, args);

        DiskService yandexDiskService = context.getBean(DiskService.class);
        FormService formService = context.getBean(FormService.class);

        String fullFileName = "/foldir/file2.docx";
        InputStream is = new ClassPathResource("./files/test.docx").getInputStream();
        yandexDiskService.upload(is, fullFileName);


//            String path = "/foldir/file1.docx";
//            System.out.println(yandexDiskService.download(path));


//        String href = "https://forms.yandex.ru/u/files?path=%2F4412411%2F693be033e010db2b8c79729f_oprosniktest.docx";
//        System.out.println(yandexDiskService.downloadForLink(href));

    }

}

//            String folderName = "foldir";
//            yandexDiskService.createDirectory(folderName);
