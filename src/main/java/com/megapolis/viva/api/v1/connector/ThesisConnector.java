package com.megapolis.viva.api.v1.connector;

import com.megapolis.viva.api.v1.dto.ThesisEmployeeDto;
import com.megapolis.viva.api.v1.dto.ThesisTaskDto;
import com.megapolis.viva.api.v1.dto.ThesisTaskIdDto;
import com.megapolis.viva.jpa.models.Employee;
import com.megapolis.viva.jpa.models.Orders;
import com.megapolis.viva.jpa.models.Template;
import com.megapolis.viva.jpa.repositories.TemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Общение с тезисом
 */

@Slf4j
@Component
public class ThesisConnector {

    @Value("${thesis.address}")
    private String address;

    @Value("${thesis.port:8080}")
    private String port;

    @Value("${thesis.task-initiator:admin}")
    private String initiator;

    @Value("${thesis.default-task-executor}")
    private String defaultExecutor;

    private final TemplateRepository templateRepo;

    public ThesisConnector(TemplateRepository templateRepo) {
        this.templateRepo = templateRepo;
    }

    @PostConstruct
    private void isConfigReady() {
        if (address == null || port == null) {
            log.error("Не заполнен порт или адрес тезиса!");
        }
    }

    private String getAddress() {
        return address + ":" + port;
    }

    /**
     * Получает номер сессии пользователя, для получения данных/создания задач и других действий с тезисом
     *
     * @return идентификатор сессии
     */
    public String getAuth() {
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-type", "application/x-www-form-urlencoded");
        headers.set("Authorization", "Basic bW9iaWxlQ2xpZW50OnNlY3JldA==");

        HttpEntity request = new HttpEntity(headers);
        try {
            return rt.exchange(getAddress() + "/app-portal/api/login?u=****&p=******&l=ru",
                    HttpMethod.GET, request, String.class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Забирает из тезиса всех сотрудников(возможно отредактирую запрос) и собирает их в ThesisEmployeeDto
     *
     * @return массив сотрудников
     */
    public ThesisEmployeeDto[] getEmployers() {
        ThesisRestTemplate rt = new ThesisRestTemplate();
        HttpEntity request = new HttpEntity(null);
        try {
            String auth = getAuth();
            String address = getAddress() + "/app-portal/api/query.json?e=megapolis%24" +
                    "Employee&q=select%20e%20from%20megapolis%24Employee%20e&view=employee-viva&s="
                    + auth;
            return rt.exchange(address, HttpMethod.GET, request, ThesisEmployeeDto[].class).getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.getResponseBodyAsString();
            log.error(e.getMessage());
        }

        return null;
    }

    /**
     * Создание задачи в тезисе(вероятно на вход должна принимать сущность отзыва, из него формировать состав пользователей
     * с их ролями)
     *
     * @return идентификатор задачи, должен присваиваться отзывам, чтобы всегда можно было найти задачу которая
     * присвоена данному отзыву
     */
    public List<Orders> createTask(List<Orders> entities) {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        //Add the Jackson Message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        // Note: here we are making this converter to process any kind of response,
        // not only application/*json, which is the default behaviour
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);

        ThesisRestTemplate rt = new ThesisRestTemplate();
        rt.setMessageConverters(messageConverters);

        List<ThesisTaskDto> dtos = ordersToDto(entities);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < dtos.size(); i++) {
            HttpEntity request = new HttpEntity(dtos.get(i), headers);
            try {
                String address = getAddress() + "/app-portal/task-megapolis?s=" + getAuth();
                entities.get(i).setTaskId(rt.postForEntity(address, request, ThesisTaskIdDto.class).getBody().getId().get(0));
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                e.getResponseBodyAsString();
                log.error(e.getMessage());
            }
        }

        return entities;
    }

    /**
     * Превращает отзывы в дто для создания задачи в тезисе
     *
     * @param entities список сущностей заявок
     * @return дто для создания задачи в тезисе
     */
    private List<ThesisTaskDto> ordersToDto(List<Orders> entities) {
        return entities.stream().map(e -> {
            Template template =
                    templateRepo.getTemplateByCityAndThemeAndInstitution(e.getCity(), e.getDepartment(), e.getInstitution())
                            .orElse(null);

            return ThesisTaskDto.builder()
                    .executors(getEmployeeLoginByRole(template, Template.Role.EXECUTOR.getValue()))
                    .observers(getEmployeeLoginByRole(template, Template.Role.OBSERVER.getValue()))
                    .controllers(getEmployeeLoginByRole(template, Template.Role.CONTROLLER.getValue()))
                    .name("Задача на основе отзыва от " + e.getDateTime())
                    .description(e.getMessage() + "\n" + e.getFeedback())
                    .startTaskType(1)
                    .priority(1)
                    .initiator(initiator)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<String> getEmployeeLoginByRole(Template template, String role) {
        if(template == null) {
            if(role.equals(Template.Role.EXECUTOR.getValue())) {
                return Arrays.asList(defaultExecutor);
            } else {
                return null;
            }
        }

        return template.getEmployeesWithRole().stream()
                .filter(employee -> employee.getRole().equals(role))
                .flatMap(employee ->
                        employee.getEmployees().stream())
                .map(Employee::getLogin)
                .collect(Collectors.toList());
    }
}
