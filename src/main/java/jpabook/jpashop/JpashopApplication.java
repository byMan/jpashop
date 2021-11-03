package jpabook.jpashop;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JpashopApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpashopApplication.class, args);
	}

	@Bean
	Hibernate5Module hibernate5Module() {
//		//지연로딩으로 읽혀지는 데이터는 무시하고 JSON 데이터 생성한다.
//		return new Hibernate5Module();



		Hibernate5Module hibernate5Module = new Hibernate5Module();

		//아래와 같이 보여줄 경우 문제는 엔티티 정보를 JSON 데이터에 포함하여 생성하기 때문에 심각한 문제가 발생한다...
		//참고로 이런게 있다 정도로만 참고하고 말자.
		//지연로딩으로 읽혀져야 할 데이터까지 포함하여 JSON 데이터 생성한다.
//		hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);

		return hibernate5Module;
	}
}
