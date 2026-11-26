package app;

import data_access.DemoCourseContentGateway;
import data_access.DemoQuestionModelGateway;
import interface_adapter.ask_page.AskPageController;
import interface_adapter.ask_page.AskPagePresenter;
import interface_adapter.ask_page.AskPageViewModel;
import interface_adapter.course_question.CourseQuestionController;
import interface_adapter.course_question.CourseQuestionPresenter;
import interface_adapter.course_question.CourseQuestionViewModel;
import interface_adapter.find_resources.FindResourcesController;
import interface_adapter.find_resources.FindResourcesPresenter;
import interface_adapter.find_resources.ResourcesViewModel;
import java.util.List;
import java.util.concurrent.Executor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import use_case.ask_page.AgentGateway;
import use_case.ask_page.AskPageInteractor;
import use_case.ask_page.PageGateway;
import use_case.find_resources.FindResourcesInteractor;
import use_case.find_resources.ResourceGateway;
import use_case.question_answering.CourseQuestionInteractor;
import view.AskPageView;
import view.CourseQuestionView;
import view.ResourcesView;

/** Composition root joins existing UC2 resource discovery to UC3 selected-page Q&A. */
public final class AppBuilder {
    private AppBuilder() { }

    public static CourseQuestionView buildUc2Demo(Executor worker) {
        CourseQuestionViewModel viewModel = new CourseQuestionViewModel();
        CourseQuestionPresenter presenter = new CourseQuestionPresenter(viewModel);
        CourseQuestionInteractor interactor = new CourseQuestionInteractor(
                new DemoCourseContentGateway(), new DemoQuestionModelGateway(),
                courseId -> courseId == DemoCourseContentGateway.COURSE_ID
                        ? List.of(DemoCourseContentGateway.TIMELINE_ID) : List.of(), presenter);
        CourseQuestionController controller = new CourseQuestionController(interactor, worker, presenter);
        return new CourseQuestionView(controller, viewModel, DemoCourseContentGateway.COURSE_ID,
                "UC2 demo: simulated course and AI");
    }

    public static JTabbedPane build(ResourceGateway resources, PageGateway pages, AgentGateway agent, Executor worker) {
        JTabbedPane tabs = new JTabbedPane();
        ResourcesViewModel resourceModel = new ResourcesViewModel();
        FindResourcesPresenter resourcePresenter = new FindResourcesPresenter(resourceModel);
        FindResourcesController resourceController = new FindResourcesController(
                new FindResourcesInteractor(resources, resourcePresenter), worker, resourcePresenter);
        ResourcesView resourceView = new ResourcesView(resourceController, resourceModel, "Canvas", row -> {
            AskPageViewModel model = new AskPageViewModel();
            AskPagePresenter presenter = new AskPagePresenter(model);
            AskPageController controller = new AskPageController(
                    new AskPageInteractor(pages, agent, presenter), worker, presenter);
            tabs.setComponentAt(1, new AskPageView(controller, model, row.courseId, row.resourceId));
            tabs.setSelectedIndex(1);
        });
        tabs.addTab("Resources", resourceView);
        JPanel empty = new JPanel();
        empty.add(new JLabel("Select a Canvas Page in Resources, then choose Ask about selected page."));
        tabs.addTab("Page questions", empty);
        resourceView.loadCourses();
        return tabs;
    }
}
