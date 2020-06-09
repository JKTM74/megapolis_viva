package com.megapolis.viva;

import com.megapolis.viva.api.v1.MainApi;
import com.megapolis.viva.api.v1.dto.ThesisEmployeeDto;
import com.megapolis.viva.jpa.models.Employee;
import com.megapolis.viva.jpa.models.InstitutionForm;
import com.megapolis.viva.jpa.models.Template;
import com.megapolis.viva.jpa.models.Theme;
import com.megapolis.viva.jpa.repositories.EmployeeRepository;
import com.megapolis.viva.jpa.repositories.InstitutionFormRepository;
import com.megapolis.viva.jpa.repositories.TemplateRepository;
import com.megapolis.viva.jpa.repositories.ThemeRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//Я бы его отсюда выпилил, но он должен быть рядом с Application
@Route
public class MainView extends VerticalLayout {

    private final MainApi mainApi;
    private final EmployeeRepository employeeRepository;
    private final ThemeRepository themeRepository;
    private final TemplateRepository templateRepository;
    private final InstitutionFormRepository institutionFormRepository;

    private Grid<Employee> controllerGrid = new Grid<>(Employee.class);
    private Grid<Employee> firstObserverGrid = new Grid<>(Employee.class);
    private Grid<Employee> secondObserverGrid = new Grid<>(Employee.class);
    private Grid<Employee> secondColumnExecutorGrid = new Grid<>(Employee.class);
    private Grid<Employee.InstitutionThesis> institutionGrid = new Grid<>(Employee.InstitutionThesis.class);
    private MultiSelectListBox<String> positionSelect = new MultiSelectListBox<>();
    private Select<Theme> themeSelect = new Select<>();
    private Select<InstitutionForm> institutionSelect = new Select<>();

    private List<Theme> themesList;
    private List<Employee> employeesList;
    private List<InstitutionForm> institutionsList;

    public MainView(MainApi mainApi,
                    EmployeeRepository employeeRepository,
                    ThemeRepository themeRepository,
                    TemplateRepository templateRepository,
                    InstitutionFormRepository institutionFormRepository) {
        this.mainApi = mainApi;
        this.employeeRepository = employeeRepository;
        this.themeRepository = themeRepository;
        this.templateRepository = templateRepository;
        this.institutionFormRepository = institutionFormRepository;
        generatePage();
    }

    private void generatePage() {
        institutionGrid.removeColumnByKey("id");
        institutionGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        institutionGrid.setWidth("370px");

        secondColumnExecutorGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        secondColumnExecutorGrid.setColumns("name");
        secondColumnExecutorGrid.setWidth("300px");

        controllerGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        controllerGrid.setColumns("name");
        controllerGrid.setWidth("300px");

        firstObserverGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        firstObserverGrid.setColumns("name");
        firstObserverGrid.setWidth("300px");

        secondObserverGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        secondObserverGrid.setColumns("name");
        secondObserverGrid.setWidth("300px");

        HorizontalLayout hl = new HorizontalLayout();

        updateThemeList();
        updateEmployeeList();
        updateInstitutionsList();

        hl.add(initFirstExecutorsColumn(employeesList), initSecondExecutorsColumn(employeesList),
                initFirstObserversColumn(employeesList), initSecondObserverColumn(employeesList, themesList), initControllerColumn(employeesList));

        add(initThemeAndEmployers(), initCityInstitutionTheme(employeesList, themesList), hl);
    }

    private void updateThemeList() {
        themesList = themeRepository.findAllByOrderByNameDesc();
    }

    private void updateEmployeeList() {
        employeesList = employeeRepository.findAllByIsActive(true);
    }

    private void updateInstitutionsList() {
        institutionsList = institutionFormRepository.findAll();
    }

    private Component initCityInstitutionTheme(List<Employee> employees, List<Theme> themes) {
        HorizontalLayout hl = new HorizontalLayout();

        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));

        VerticalLayout vlCity = new VerticalLayout(cityComment, citySelect);

        Label departmentComment = new Label("Заведение:");
        institutionSelect.setItems(institutionFormRepository.findAll());

        Button deleteInstitutionButton = new Button("Удалить выбранное заведение", b -> {
            institutionFormRepository.delete(institutionSelect.getValue());
            Notification.show("Тема успешно удалена!");
            institutionsList.remove(institutionSelect.getValue());
            institutionSelect.setItems(institutionsList);
        });
        deleteInstitutionButton.setWidthFull();

        HorizontalLayout hlInstitution = new HorizontalLayout(institutionSelect, deleteInstitutionButton);
        VerticalLayout vlInstitution = new VerticalLayout(departmentComment, hlInstitution);

        Label themeComment = new Label("Тема:");
        themeSelect.setItems(themes);

        Button deleteThemeButton = new Button("Удалить выбранную тему", b -> {
            themeRepository.delete(themeSelect.getValue());
            Notification.show("Тема успешно удалена!");
            themes.remove(themeSelect.getValue());
            themeSelect.setItems(themes);
        });
        deleteThemeButton.setWidthFull();

        HorizontalLayout hlTheme = new HorizontalLayout(themeSelect, deleteThemeButton);
        VerticalLayout vlTheme = new VerticalLayout(themeComment, hlTheme);

        Button addTemplateButton = new Button("Создать шаблон!", b -> {
            List<Template.EmployeesWithRole> employeesWithRoles = new ArrayList<>();

            //Добавляем контроллёров
            employeesWithRoles.add(
                    Template.EmployeesWithRole.builder()
                            .role(Template.Role.CONTROLLER.getValue())
                            .employees(controllerGrid.getSelectedItems())
                            .build());

            //Добавляем наблюдателей
            Set<Employee> observersSet = new HashSet<>(firstObserverGrid.getSelectedItems());
            observersSet.addAll(secondObserverGrid.getSelectedItems());
            employeesWithRoles.add(
                    Template.EmployeesWithRole.builder()
                            .role(Template.Role.OBSERVER.getValue())
                            .employees(observersSet)
                            .build());

            //Добавляем исполнителей
            Set<Employee> executorsSet = new HashSet<>(secondColumnExecutorGrid.getSelectedItems());
            //Я тоже себя за это осуждаю
            for (Employee.InstitutionThesis institution : institutionGrid.getSelectedItems()) {
                for (Employee employee : employees) {
                    if (employee.getInstitutions().contains(institution)) {
                        for (String position : positionSelect.getSelectedItems()) {
                            if (employee.getPosition().equals(position)) {
                                executorsSet.add(employee);
                            }
                        }
                    }
                }
            }
            employeesWithRoles.add(
                    Template.EmployeesWithRole.builder()
                            .role(Template.Role.EXECUTOR.getValue())
                            .employees(executorsSet)
                            .build());

            List<Template> templates = templateRepository.findAll();
            for (Template template : templates) {
                if (template.getCity().equals(citySelect.getValue())
                        && template.getInstitutionForm().getName().equals(institutionSelect.getValue().getName())
                        && template.getTheme().getName().equals(themeSelect.getValue().getName())) {
                    Notification.show("Шаблон с такой комбинацией город + заведение + тема уже существует!");
                    return;
                }
            }

            templateRepository.save(
                    Template.builder()
                            .city(citySelect.getValue())
                            .institutionForm(institutionSelect.getValue())
                            .theme(themeSelect.getValue())
                            .employeesWithRole(employeesWithRoles)
                            .build()
            );

            Notification.show("Шаблон успешно создан!");
        });
        addTemplateButton.setWidthFull();
        addTemplateButton.getStyle().set("margin-top", "62px");

        hl.add(vlCity, vlInstitution, vlTheme, addTemplateButton);
        hl.setSizeFull();
        return hl;
    }

    private Component initControllerColumn(List<Employee> employees) {
        VerticalLayout vl = new VerticalLayout();

        Label rowComment = new Label("Контролёры");


        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));
        citySelect.addValueChangeListener(event -> {
                    controllerGrid.setItems(employees.stream().filter(e -> e.getCity().equals(citySelect.getValue())));
                }
        );

        Label employeeComment = new Label("Сотрудники");

        vl.add(rowComment, cityComment, citySelect, employeeComment, controllerGrid);

        return vl;
    }

    private Component initFirstObserversColumn(List<Employee> employees) {
        VerticalLayout vl = new VerticalLayout();

        Label rowComment = new Label("Первая группа наблюдателей");
        Select<Employee.InstitutionThesis> institutionSelect = new Select<>();

        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));
        citySelect.addValueChangeListener(event -> {
            institutionSelect.setItems(employees.stream()
                    .filter(e -> e.getCity().equals(citySelect.getValue()))
                    .flatMap(e -> e.getInstitutions().stream())
                    .sorted(Comparator.comparing(Employee.InstitutionThesis::getName))
                    .filter(distinctByKey(Employee.InstitutionThesis::getName)));
        });

        Label institutionComment = new Label("Заведение:");
        institutionSelect.addValueChangeListener(event -> {
            Set<Employee> filteredEmployees = employees.stream()
                    .filter(e -> e.getInstitutions() != null && e.getCity() != null &&
                            e.getInstitutions().stream().map(Employee.InstitutionThesis::getName)
                                    .collect(Collectors.toList()).
                                    contains(institutionSelect.getValue().getName()) && e.getCity().equals(citySelect.getValue()))
                    .collect(Collectors.toSet());
            firstObserverGrid.setItems(!CollectionUtils.isEmpty(filteredEmployees) ? filteredEmployees : new HashSet<>());
        });

        Label employeeComment = new Label("Сотрудники");

        vl.add(rowComment, cityComment, citySelect, institutionComment, institutionSelect, employeeComment, firstObserverGrid);

        return vl;
    }

    private Component initSecondObserverColumn(List<Employee> employees, List<Theme> themes) {
        VerticalLayout vl = new VerticalLayout();

        Label rowComment = new Label("Второй группа наблюдателей");

        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));
        citySelect.addValueChangeListener(event -> {
                    secondObserverGrid.setItems(employees.stream().filter(e -> e.getCity().equals(citySelect.getValue())));
                }
        );

        Label employeeComment = new Label("Сотрудники");

        vl.add(rowComment, cityComment, citySelect, employeeComment, secondObserverGrid);

        return vl;
    }

    private Component initSecondExecutorsColumn(List<Employee> employees) {
        VerticalLayout vl = new VerticalLayout();

        Label rowComment = new Label("Вторая группа исполнителей");

        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));
        citySelect.addValueChangeListener(event -> {
                    secondColumnExecutorGrid.setItems(employees.stream().filter(e -> e.getCity().equals(citySelect.getValue())));
                }
        );


        Label employeeComment = new Label("Сотрудники");

        vl.add(rowComment, cityComment, citySelect, employeeComment, secondColumnExecutorGrid);

        return vl;
    }

    private Component initFirstExecutorsColumn(List<Employee> employees) {
        VerticalLayout vl = new VerticalLayout();

        List<Employee.InstitutionThesis> totalInstitutionList = new ArrayList<>();
        for (Employee employee : employees) {
            totalInstitutionList.addAll(employee.getInstitutions());
        }

        Label rowComment = new Label("Первая группа исполнителей");

        Label cityComment = new Label("Город:");
        Select<String> citySelect = new Select<>();
        citySelect.setItems(employees.stream().map(Employee::getCity).distinct().collect(Collectors.toList()));
        citySelect.addValueChangeListener(event -> {
            List<Employee.InstitutionThesis> employeesFromChoosenCity =
                    employees.stream()
                            .filter(e -> e.getCity().equals(citySelect.getValue()))
                            .flatMap(e -> e.getInstitutions().stream())
                            .filter(distinctByKey(Employee.InstitutionThesis::getName))
                            .collect(Collectors.toList());
            institutionGrid.setItems(employeesFromChoosenCity);
        });

        Button showPositions = new Button("Показать должности!", e -> {
            Set<String> totalPositionList = new HashSet<>();
            for (Employee.InstitutionThesis institution : institutionGrid.getSelectedItems()) {
                for (Employee employee : employees) {
                    for (Employee.InstitutionThesis inst : employee.getInstitutions()) {
                        if (inst.getName().equals(institution.getName())) {
                            totalPositionList.add(employee.getPosition());
                            break;
                        }
                    }
                }
            }
            positionSelect.setItems(totalPositionList);
        });

        Label positionComment = new Label("Должность:");

        Label institutionComment = new Label("Заведение:");

        vl.add(rowComment, cityComment, citySelect, institutionComment, institutionGrid, showPositions, positionComment, positionSelect);

        return vl;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private Component initThemeAndEmployers() {
        HorizontalLayout hl = new HorizontalLayout();
        Button downloadEmployeesFromThesis = new Button("Загрузка сотрудников из тезиса в базу!", e -> {
            List<ThesisEmployeeDto> dtos = mainApi.getEmployees();
            Notification.show("Сотрудники успешно загружены");
            UI.getCurrent().getPage().reload();
        });

        hl.setSizeFull();

        TextArea themeTextArea = new TextArea();
        themeTextArea.setHeight(downloadEmployeesFromThesis.getHeight());

        Button themeButton = new Button("Добавить тему", e -> {
            if (StringUtils.isEmpty(themeTextArea.getValue())) {
                Notification.show("Не введено наименование темы!");
                return;
            }

            Theme theme = Theme.builder()
                    .name(themeTextArea.getValue()).build();
            themeRepository.save(theme);
            themesList.add(theme);
            themeSelect.setItems(themesList);

            Notification.show("Тема добавлена");
        });

        TextArea institutionTextArea = new TextArea();
        institutionTextArea.setHeight(downloadEmployeesFromThesis.getHeight());

        Button institutionButton = new Button("Добавить заведение", e -> {
            if (StringUtils.isEmpty(institutionTextArea.getValue())) {
                Notification.show("Не введено наименование заведения!");
                return;
            }

            InstitutionForm institution = InstitutionForm.builder()
                    .name(institutionTextArea.getValue()).build();
            institutionFormRepository.save(institution);
            institutionsList.add(institution);
            institutionSelect.setItems(institutionsList);

            Notification.show("Заведение добавлено!");
        });

        Button toTemplateButton = new Button("Перейти к просмотру шаблонов", e -> {
            UI.getCurrent().navigate("template");
        });

        VerticalLayout vlTemplate = new VerticalLayout(toTemplateButton);
        vlTemplate.setDefaultHorizontalComponentAlignment(Alignment.END);
        vlTemplate.setWidth("600px");

        hl.add(downloadEmployeesFromThesis, themeTextArea, themeButton, institutionTextArea, institutionButton, vlTemplate);
        hl.getStyle().set("border-bottom", "1px solid black");
        return hl;
    }

}
