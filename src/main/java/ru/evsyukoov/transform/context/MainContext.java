package ru.evsyukoov.transform.context;

import com.ibm.icu.text.Transliterator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.evsyukoov.transform.dto.FileInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MainContext {

    @Bean("executor")
    ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor threadPoolExecutor =  new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(10);
        threadPoolExecutor.setMaxPoolSize(100);
        threadPoolExecutor.setKeepAliveSeconds(120);
        threadPoolExecutor.setQueueCapacity(100);
        return threadPoolExecutor;
    }

    @Bean
    Transliterator latinToCyrillic() {
        return Transliterator.getInstance("Latin-Russian/BGN");
    }

    @Bean
    Transliterator cyrillicToLatin() {
        return Transliterator.getInstance("Russian-Latin/BGN");
    }

    /**
     * @return - мапа для хранения распаршенного файла, который отправил клиент
     * На диск файл также кладем, чтобы в случае ненахода в кеше считать с диска
     */
    @Bean
    Map<Long, FileInfo> clientFileCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        return dbFactory.newDocumentBuilder();
    }

}
