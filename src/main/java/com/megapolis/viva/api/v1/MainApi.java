package com.megapolis.viva.api.v1;

import com.megapolis.viva.api.v1.connector.SheetConnector;
import com.megapolis.viva.api.v1.connector.ThesisConnector;
import com.megapolis.viva.api.v1.dto.ThesisEmployeeDto;
import com.megapolis.viva.jpa.models.Employee;
import com.megapolis.viva.jpa.models.Orders;
import com.megapolis.viva.jpa.repositories.EmployeeRepository;
import com.megapolis.viva.jpa.repositories.OrdersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
@EnableScheduling
public class MainApi {

    private final ThesisConnector thesisConnector;
    private final SheetConnector sheetConnector;
    private final EmployeeRepository employeeRepository;
    private final OrdersRepository ordersRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss", Locale.ENGLISH);

    private HashMap<String, LocalDateTime> maxDateTimeByCity = new HashMap<>();

    @Value("${sheet.sheet-names}")
    private String[] sheetNames;

    MainApi(ThesisConnector thesisConnector,
            SheetConnector sheetConnector,
            EmployeeRepository employeeRepository,
            OrdersRepository ordersRepository) {
        this.thesisConnector = thesisConnector;
        this.sheetConnector = sheetConnector;
        this.employeeRepository = employeeRepository;
        this.ordersRepository = ordersRepository;
    }

    /**
     * Этот метод опрашивает шит и забирает оттуда задачи
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Scheduled(fixedRateString = "${sheet.scheduler-timer-ms: 10000}", initialDelay = 1000)
    public void getDataFromSheet() throws GeneralSecurityException, IOException {

        for (String city : sheetNames) {
            List<List<Object>> dataFromSheet = sheetConnector.getOrders(city);
            setMaxDateTimeByCityFromSheet(city, dataFromSheet);

            List<Orders> orders = ordersRepository.getOrdersWithTaskIdIsNull() == null
                    ? new ArrayList<>()
                    : ordersRepository.getOrdersWithTaskIdIsNull();

            for (List row : dataFromSheet) {

                try {
                    LocalDateTime orderDate = LocalDateTime.parse(row.get(0).toString(), formatter);

                    //если вдруг поля изменятся в шите то в первую очередь нужно менять здесь
                    if (orderDate.isAfter(getMaxDateTimeFromDB(city)))
                        orders.add(Orders.builder()
                                .dateTime(orderDate)
                                .city(row.get(1) != null ? row.get(1).toString() : "")
                                .institution(row.get(2) != null ? row.get(2).toString() : "")
                                .type(row.get(3) != null ? row.get(3).toString() : "")
                                .department(row.get(4) != null ? row.get(4).toString() : "")
                                .message(row.get(5) != null ? row.get(5).toString() : "")
                                .feedback(row.get(6) != null ? row.get(6).toString() : "")
                                .build());
                } catch (DateTimeParseException ed) {
                    log.error("Invalid date from sheet. " + ed.getMessage());
                } catch (ArrayIndexOutOfBoundsException ea) {
                    log.error("Invalid data from sheet. " + ea.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

            if (!CollectionUtils.isEmpty(orders)) {
                //сохраняем отзывы без идентификатора задачи в тезисе
                ordersRepository.saveAll(orders);
                //сохраняем идентификаторы задач созданных на основе отзывов
                ordersRepository.saveAll(thesisConnector.createTask(orders));
            }

            log.info("list size of new orders {} by city {}", orders.size(), city);
        }
    }

    /**
     * Берет максимальную дату по городу, нужно для того чтобы создать задачи > max_time
     * Пример, последний отзыв был от 01.01.2020 09:15, метод вернёт это время
     * @param city
     * @return
     */
    private LocalDateTime getMaxDateTimeFromDB(String city) {
        /**
         * Костыль для Нижнего Тагила. Лист в гугл таблицах называется "Н.Тагил", а в поле "Мой город" у всех записей "Нижний Тагил".
         * В БД тоже падают с "Нижний Тагил". Чтобы поиск максимальной даты в БД был правильным, вот так вот.
         */
        LocalDateTime dateTimeFromDB = city.equals("Н.Тагил") ? ordersRepository.findMaximumDateByCityNative("Нижний Тагил") : ordersRepository.findMaximumDateByCityNative(city);

        return dateTimeFromDB != null ? dateTimeFromDB : maxDateTimeByCity.get(city);
    }

    /**
     * Этот метод нужен на в первую на тот случай если у нас нет в базе ни одной записи, он поместит в мапу отзыв
     * с последней датой и мапа будет использоваться в com.megapolis.viva.api.v1.MainApi#getMaxDateTimeFromDB(java.lang.String)
     * @param city страница с отзывами в шите(она названа также как город)
     * @param dataFromSheet отзывы
     */
    private void setMaxDateTimeByCityFromSheet(String city, List<List<Object>> dataFromSheet) {
        if (getMaxDateTimeFromDB(city) == null && !maxDateTimeByCity.containsKey(city)) {
            LocalDateTime localDateTime = LocalDateTime.MIN;

            for (List row : dataFromSheet) {
                try {
                    LocalDateTime orderDate = LocalDateTime.parse(row.get(0).toString(), formatter);

                    if (localDateTime.isBefore(orderDate))
                        localDateTime = orderDate;

                } catch (Exception e) {
                    e.getMessage();
                }
            }
            maxDateTimeByCity.put(city, localDateTime);
        }
    }

    /**
     * Запрашиваем сотрудников из тезиса и сохраняем их в базу
     * @return дто сотрудников
     */
    public List<ThesisEmployeeDto> getEmployees() {
        List<ThesisEmployeeDto> listDtos = Arrays.asList(thesisConnector.getEmployers());

        //Собираем наши сущности из дто и сохраняем в базу
        List<Employee> employees = listDtos.stream()
                .map(dto ->
                        Employee.builder()
                                .id(dto.getId())
                                .city(dto.getCity() != null ? dto.getCity().getName() : "")
                                .login(dto.getUser().getLogin())
                                .name(dto.getName() != null ? dto.getName() : "")
                                .patronymic(dto.getMiddleName() != null ? dto.getMiddleName() : "")
                                .position(dto.getUser().getPosition() != null ? dto.getUser().getPosition() : "")
                                .surname(dto.getLastName() != null ? dto.getLastName() : "")
                                .institutions(dto.getInstitutions() != null ? dto.getInstitutions().stream()
                                        .map(t -> Employee.InstitutionThesis.builder()
                                                .id(t.getId())
                                                .name(t.getName())
                                                .build()).collect(Collectors.toList())
                                        : new ArrayList<>())
                                .isActive(Boolean.valueOf(dto.getUser().getActive()))
                                .build()).collect(Collectors.toList());

        employeeRepository.saveAll(employees);

        return listDtos;
    }
}

