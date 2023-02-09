package ru.evsyukoov.transform.context;

import com.ibm.icu.text.Transliterator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
}
