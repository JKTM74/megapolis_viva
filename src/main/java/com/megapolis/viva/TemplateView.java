package com.megapolis.viva;

import com.megapolis.viva.jpa.models.Employee;
import com.megapolis.viva.jpa.models.Template;
import com.megapolis.viva.jpa.repositories.TemplateRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Route(value = "template")
public class TemplateView extends VerticalLayout {

    private final TemplateRepository templateRepository;

    public TemplateView(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        generatePage();
    }

    private void generatePage() {
        List<Template> templateList = templateRepository.findAll();

        VerticalLayout hl = new VerticalLayout(new Button("Перейти к созданию шаблона!", e -> {
            UI.getCurrent().navigate("");
        }));
        hl.setDefaultHorizontalComponentAlignment(Alignment.END);

        add(hl);

        for (Template template : templateList) {
            add(generateRow(template));
        }
    }

    private Component generateRow(Template template) {
        HorizontalLayout hl = new HorizontalLayout();

        Label cityTextField = new Label(template.getCity());
        Label themeTextField = new Label(template.getTheme().getName());
        Label institutionTextField = new Label(template.getInstitutionForm().getName());

        Grid<GridEmployeeView> employeesGrid = new Grid<>(GridEmployeeView.class);
        employeesGrid.setWidth("800px");
        employeesGrid.setHeight("200px");
        List<GridEmployeeView> gridEmployeeViews = new ArrayList<>();
        for (Template.EmployeesWithRole er : template.getEmployeesWithRole()) {
            String role = er.getRole();
            for (Employee employee : er.getEmployees()) {
                gridEmployeeViews.add(GridEmployeeView.builder()
                        .role(role)
                        .employee(employee)
                        .build());
            }
        }
        employeesGrid.setItems(gridEmployeeViews);

        Button deleteTemplateButton = new Button("Удалить шаблон", e -> {
            templateRepository.delete(template);
            Notification.show("Шаблон успешно удалён!");
            UI.getCurrent().getPage().reload();
        });

        hl.add(cityTextField, themeTextField, institutionTextField, deleteTemplateButton);
        VerticalLayout vl = new VerticalLayout(hl);

        vl.add(employeesGrid);
        vl.getStyle().set("border-bottom" , "6px dotted DarkOrange");
        return vl;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GridEmployeeView {
        private String role;
        private Employee employee;
    }

}
