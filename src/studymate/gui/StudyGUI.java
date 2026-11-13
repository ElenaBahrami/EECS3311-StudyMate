package studymate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import studymate.agent.LearningAgent;
import studymate.agent.OpenAIAdapter;
import studymate.agent.RetrievalTool;
import studymate.agent.ToolRegistry;
import studymate.controller.AgentController;
import studymate.extraction.TextExtractor;
import studymate.model.AvailabilitySlot;
import studymate.model.Course;
import studymate.model.LearningArtifact;
import studymate.model.Material;
import studymate.model.StudyPlan;
import studymate.model.StudySession;
import studymate.planning.DeadlineStrategy;
import studymate.planning.SchedulingStrategy;
import studymate.planning.WeakTopicStrategy;
import studymate.repository.StudyRepository;
import studymate.service.MaterialService;

public class StudyGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // =====================================================
    // BACKEND
    // =====================================================

    private final StudyRepository repository;
    private final Course course;
    private final MaterialService materialService;
    private final LearningAgent learningAgent;
    private final AgentController controller;

    // =====================================================
    // GUI STATE
    // =====================================================

    private JLabel statusLabel;

    public StudyGUI() {

        // -------------------------------------------------
        // DATABASE + COURSE
        // -------------------------------------------------

        repository =
                new StudyRepository();

        course =
                repository.getOrCreateCourse(
                        "EECS3311 Software Design"
                );

        // -------------------------------------------------
        // F01 MATERIAL BACKEND
        // -------------------------------------------------

        TextExtractor extractor =
                new TextExtractor();

        materialService =
                new MaterialService(
                        repository,
                        extractor
                );

        // -------------------------------------------------
        // AI BACKEND
        // -------------------------------------------------

        ToolRegistry toolRegistry =
                new ToolRegistry();

        toolRegistry.register(
                "retrieve",
                new RetrievalTool(
                        repository
                )
        );

        OpenAIAdapter openAI =
                new OpenAIAdapter();

        learningAgent =
                new LearningAgent(
                        openAI,
                        toolRegistry,
                        repository
                );

        // -------------------------------------------------
        // F10 CONTROLLER
        // -------------------------------------------------

        controller =
                new AgentController(
                        learningAgent,
                        repository
                );

        // -------------------------------------------------
        // WINDOW
        // -------------------------------------------------

        setTitle(
                "StudyMate - EECS3311 Software Design"
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setMinimumSize(
                new Dimension(
                        1000,
                        700
                )
        );

        setSize(
                1100,
                750
        );

        setLocationRelativeTo(
                null
        );

        buildInterface();
    }

    // =====================================================
    // INTERFACE
    // =====================================================

    private void buildInterface() {

        setLayout(
                new BorderLayout(
                        10,
                        10
                )
        );

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        add(
                createTabs(),
                BorderLayout.CENTER
        );

        add(
                createStatusBar(),
                BorderLayout.SOUTH
        );
    }

    // =====================================================
    // HEADER
    // =====================================================

    private JPanel createHeader() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        20,
                        5,
                        20
                )
        );

        JLabel title =
                new JLabel(
                        "StudyMate"
                );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        28
                )
        );

        JLabel courseLabel =
                new JLabel(
                        course.getName(),
                        SwingConstants.RIGHT
                );

        courseLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        16
                )
        );

        panel.add(
                title,
                BorderLayout.WEST
        );

        panel.add(
                courseLabel,
                BorderLayout.EAST
        );

        return panel;
    }

    // =====================================================
    // TABS
    // =====================================================

    private JTabbedPane createTabs() {

        JTabbedPane tabs =
                new JTabbedPane();

        tabs.addTab(
                "Materials",
                createMaterialsPanel()
        );

        tabs.addTab(
                "AI Study",
                createAIStudyPanel()
        );

        tabs.addTab(
                "Study Plan",
                createPlanPanel()
        );

        tabs.addTab(
                "Flashcards",
                createFlashcardsPanel()
        );

        tabs.addTab(
                "Quiz",
                createQuizPanel()
        );

        tabs.addTab(
                "Progress",
                createProgressPanel()
        );

        return tabs;
    }

    // =====================================================
    // F01 - MATERIALS
    // =====================================================

    private JPanel createMaterialsPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Course Materials",
                        "Import and view PDF/TXT course materials."
                );

        JButton importButton =
                new JButton(
                        "Import Material"
                );

        JButton viewButton =
                new JButton(
                        "View Saved Materials"
                );

        importButton.addActionListener(
                e -> importMaterial(
                        feature.outputArea
                )
        );

        viewButton.addActionListener(
                e -> showSavedMaterials(
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                importButton,
                viewButton
        );

        return feature.panel;
    }

    private void importMaterial(
            JTextArea outputArea) {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select Course Material"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "PDF and Text Files (*.pdf, *.txt)",
                        "pdf",
                        "txt"
                )
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        int choice =
                chooser.showOpenDialog(
                        this
                );

        if (choice
                != JFileChooser.APPROVE_OPTION) {

            statusLabel.setText(
                    "Import cancelled"
            );

            return;
        }

        File selectedFile =
                chooser.getSelectedFile();

        if (selectedFile == null) {

            return;
        }

        outputArea.setText(
                "Importing material...\n\n"
                        + selectedFile.getAbsolutePath()
        );

        statusLabel.setText(
                "Importing material..."
        );

        try {

            Material material =
                    materialService
                            .importMaterial(
                                    course.id(),
                                    selectedFile
                                            .getAbsolutePath()
                            );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "IMPORT SUCCESSFUL\n"
            );

            output.append(
                    "==============================\n\n"
            );

            output.append(
                    "Material: "
                            + material.getTitle()
                            + "\n"
            );

            output.append(
                    "Chunks: "
                            + material
                                    .chunks()
                                    .size()
                            + "\n"
            );

            output.append(
                    "Material ID: "
                            + material.getId()
                            + "\n\n"
            );

            output.append(
                    "The material was saved "
                            + "to the StudyMate database."
            );

            outputArea.setText(
                    output.toString()
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Material imported successfully"
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Material imported successfully.",
                    "StudyMate",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {

            outputArea.setText(
                    "IMPORT FAILED\n"
                            + "==============================\n\n"
                            + safeMessage(ex)
            );

            statusLabel.setText(
                    "Import failed"
            );

            JOptionPane.showMessageDialog(
                    this,
                    safeMessage(ex),
                    "Import Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showSavedMaterials(
            JTextArea outputArea) {

        try {

            List<Material> materials =
                    repository
                            .getMaterialsByCourse(
                                    course.id()
                            );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "SAVED COURSE MATERIALS\n"
            );

            output.append(
                    "==============================\n\n"
            );

            output.append(
                    "Course: "
                            + course.getName()
                            + "\n\n"
            );

            output.append(
                    "Total materials: "
                            + materials.size()
                            + "\n\n"
            );

            if (materials.isEmpty()) {

                output.append(
                        "No materials have been imported yet."
                );

            } else {

                int number =
                        1;

                for (Material material :
                        materials) {

                    output.append(
                            number
                                    + ". "
                                    + material.getTitle()
                                    + "\n"
                    );

                    output.append(
                            "   ID: "
                                    + material.getId()
                                    + "\n"
                    );

                    output.append(
                            "   Chunks: "
                                    + material
                                            .chunks()
                                            .size()
                                    + "\n\n"
                    );

                    number++;
                }
            }

            outputArea.setText(
                    output.toString()
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Loaded "
                            + materials.size()
                            + " saved material(s)"
            );

        } catch (Exception ex) {

            outputArea.setText(
                    "Unable to load materials.\n\n"
                            + safeMessage(ex)
            );

            statusLabel.setText(
                    "Unable to load materials"
            );
        }
    }

    // =====================================================
    // F02 / F03 / F04 - AI STUDY
    // =====================================================

    private JPanel createAIStudyPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "AI Study Tools",
                        "Generate grounded summaries, concepts and course answers."
                );

        JButton summaryButton =
                new JButton(
                        "Summarise Materials"
                );

        JButton conceptsButton =
                new JButton(
                        "Extract Key Concepts"
                );

        JButton questionButton =
                new JButton(
                        "Ask a Question"
                );

        summaryButton.addActionListener(
                e -> runArtifactTask(
                        summaryButton,
                        feature.outputArea,
                        "Generating grounded summary...",
                        "AI SUMMARY",
                        () ->
                                learningAgent.summarise(
                                        course.id()
                                )
                )
        );

        conceptsButton.addActionListener(
                e -> runArtifactTask(
                        conceptsButton,
                        feature.outputArea,
                        "Extracting grounded key concepts...",
                        "KEY CONCEPTS",
                        () ->
                                learningAgent
                                        .extractConcepts(
                                                course.id()
                                        )
                )
        );

        questionButton.addActionListener(
                e -> askCourseQuestion(
                        questionButton,
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                summaryButton,
                conceptsButton,
                questionButton
        );

        return feature.panel;
    }

    private void askCourseQuestion(
            JButton button,
            JTextArea outputArea) {

        String question =
                JOptionPane.showInputDialog(
                        this,
                        "Enter your question about "
                                + "the course material:",
                        "Ask StudyMate",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (question == null) {

            statusLabel.setText(
                    "Question cancelled"
            );

            return;
        }

        question =
                question.trim();

        if (question.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a question.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        final String submittedQuestion =
                question;

        runArtifactTask(
                button,
                outputArea,
                "Searching course evidence "
                        + "and generating an answer...",
                "COURSE QUESTION & ANSWER",
                () ->
                        learningAgent.answerQuestion(
                                course.id(),
                                submittedQuestion
                        ),
                "Question: "
                        + submittedQuestion
                        + "\n\n"
        );
    }

    private void runArtifactTask(
            JButton button,
            JTextArea outputArea,
            String runningMessage,
            String heading,
            Callable<LearningArtifact> task) {

        runArtifactTask(
                button,
                outputArea,
                runningMessage,
                heading,
                task,
                ""
        );
    }

    private void runArtifactTask(
            JButton button,
            JTextArea outputArea,
            String runningMessage,
            String heading,
            Callable<LearningArtifact> task,
            String prefix) {

        button.setEnabled(
                false
        );

        outputArea.setText(
                runningMessage
        );

        statusLabel.setText(
                runningMessage
        );

        SwingWorker<LearningArtifact, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected LearningArtifact doInBackground()
                            throws Exception {

                        return task.call();
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            LearningArtifact artifact =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    heading
                            );

                            output.append(
                                    "\n==============================\n\n"
                            );

                            if (prefix != null
                                    && !prefix.isBlank()) {

                                output.append(
                                        prefix
                                );
                            }

                            output.append(
                                    artifact.content()
                            );

                            output.append(
                                    "\n\n==============================\n"
                            );

                            List<UUID> sourceIds =
                                    artifact.getSourceIds();

                            output.append(
                                    "Sources used: "
                                            + sourceIds.size()
                                            + "\n"
                            );

                            if (!sourceIds.isEmpty()) {

                                output.append(
                                        "\nSource IDs:\n"
                                );

                                int number =
                                        1;

                                for (UUID sourceId :
                                        sourceIds) {

                                    output.append(
                                            number
                                                    + ". "
                                                    + sourceId
                                                    + "\n"
                                    );

                                    number++;
                                }
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                            statusLabel.setText(
                                    heading
                                            + " generated successfully"
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showAITaskError(
                                    outputArea,
                                    "Operation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            String message =
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause);

                            showAITaskError(
                                    outputArea,
                                    message
                            );
                        }
                    }
                };

        worker.execute();
    }

    // =====================================================
    // F05 + F10 - STUDY PLAN PANEL
    // =====================================================

    private JPanel createPlanPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Study Planning",
                        "Generate, preview, confirm and undo study-plan revisions."
                );

        JButton generateButton =
                new JButton(
                        "Generate Study Plan"
                );

        JButton latestButton =
                new JButton(
                        "Show Latest Plan"
                );

        JButton adaptButton =
                new JButton(
                        "Adapt Plan"
                );

        JButton confirmButton =
                new JButton(
                        "Confirm Revision"
                );

        JButton rejectButton =
                new JButton(
                        "Reject Revision"
                );

        JButton undoButton =
                new JButton(
                        "Undo Revision"
                );

        // F05
        generateButton.addActionListener(
                e -> generateStudyPlanGUI(
                        generateButton,
                        feature.outputArea
                )
        );

        // Show latest persisted plan
        latestButton.addActionListener(
                e -> showLatestPlan(
                        feature.outputArea
                )
        );

        // F10 Preview
        adaptButton.addActionListener(
                e -> previewPlanRevision(
                        adaptButton,
                        feature.outputArea
                )
        );

        // F10 Confirm
        confirmButton.addActionListener(
                e -> confirmPlanRevision(
                        feature.outputArea
                )
        );

        // F10 Reject
        rejectButton.addActionListener(
                e -> rejectPlanRevision(
                        feature.outputArea
                )
        );

        // F10 Undo
        undoButton.addActionListener(
                e -> undoPlanRevision(
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                generateButton,
                latestButton,
                adaptButton,
                confirmButton,
                rejectButton,
                undoButton
        );

        return feature.panel;
    }

    // =====================================================
    // F05 - GENERATE STUDY PLAN
    // =====================================================

    private void generateStudyPlanGUI(
            JButton button,
            JTextArea outputArea) {

        PlanInput input;

        try {

            input =
                    requestPlanInput(
                            "Generate Study Plan"
                    );

        } catch (IllegalArgumentException ex) {

            showPlanInputError(
                    ex.getMessage()
            );

            return;
        }

        if (input == null) {

            statusLabel.setText(
                    "Study-plan generation cancelled"
            );

            return;
        }

        button.setEnabled(
                false
        );

        outputArea.setText(
                "Generating grounded topic estimates "
                        + "and scheduling sessions..."
        );

        statusLabel.setText(
                "Generating study plan..."
        );

        SwingWorker<StudyPlan, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected StudyPlan doInBackground()
                            throws Exception {

                        return learningAgent
                                .generateStudyPlan(
                                        course.id(),
                                        input.examDate,
                                        input.availability,
                                        input.strategy
                                );
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            StudyPlan plan =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    "GENERATED STUDY PLAN\n"
                            );

                            output.append(
                                    "==============================\n"
                            );

                            output.append(
                                    formatStudyPlan(
                                            plan
                                    )
                            );

                            output.append(
                                    "\n"
                            );

                            if (plan.isFeasible()) {

                                output.append(
                                        "The feasible plan was saved."
                                );

                                statusLabel.setText(
                                        "Study plan generated and saved"
                                );

                            } else {

                                output.append(
                                        "The plan is not feasible "
                                                + "and was NOT saved."
                                );

                                statusLabel.setText(
                                        "Study plan has insufficient capacity"
                                );
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showPlanError(
                                    outputArea,
                                    "Study-plan generation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showPlanError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    // =====================================================
    // SHOW LATEST PLAN
    // =====================================================

    private void showLatestPlan(
            JTextArea outputArea) {

        try {

            StudyPlan plan =
                    repository
                            .findLatestPlanByCourse(
                                    course.id()
                            );

            if (plan == null) {

                outputArea.setText(
                        "LATEST STUDY PLAN\n"
                                + "==============================\n\n"
                                + "No saved study plan exists."
                );

                statusLabel.setText(
                        "No saved study plan"
                );

                return;
            }

            outputArea.setText(
                    "LATEST STUDY PLAN\n"
                            + "==============================\n"
                            + formatStudyPlan(plan)
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Latest study plan loaded"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    // =====================================================
    // F10 - ADAPT / PREVIEW
    // =====================================================

    private void previewPlanRevision(
            JButton button,
            JTextArea outputArea) {

        StudyPlan current;

        try {

            current =
                    repository
                            .findLatestPlanByCourse(
                                    course.id()
                            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );

            return;
        }

        if (current == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Generate a study plan first.",
                    "No Study Plan",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        PlanInput input;

        try {

            input =
                    requestPlanInput(
                            "Adapt Study Plan"
                    );

        } catch (IllegalArgumentException ex) {

            showPlanInputError(
                    ex.getMessage()
            );

            return;
        }

        if (input == null) {

            statusLabel.setText(
                    "Plan adaptation cancelled"
            );

            return;
        }

        button.setEnabled(
                false
        );

        outputArea.setText(
                "Generating revision preview...\n\n"
                        + "The saved plan is not being changed."
        );

        statusLabel.setText(
                "Generating revision preview..."
        );

        SwingWorker<StudyPlan, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected StudyPlan doInBackground()
                            throws Exception {

                        return controller
                                .previewRevision(
                                        course.id(),
                                        input.examDate,
                                        input.availability,
                                        input.strategy
                                );
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            StudyPlan preview =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    "REVISION PREVIEW\n"
                            );

                            output.append(
                                    "==============================\n"
                            );

                            output.append(
                                    formatStudyPlan(
                                            preview
                                    )
                            );

                            output.append(
                                    "\n"
                            );

                            if (preview.isFeasible()) {

                                output.append(
                                        "PREVIEW ONLY.\n"
                                );

                                output.append(
                                        "The saved plan has NOT changed.\n\n"
                                );

                                output.append(
                                        "Use \"Confirm Revision\" "
                                                + "to save this preview,\n"
                                );

                                output.append(
                                        "or \"Reject Revision\" "
                                                + "to discard it."
                                );

                                statusLabel.setText(
                                        "Revision preview ready"
                                );

                            } else {

                                output.append(
                                        "This revision is not feasible.\n"
                                );

                                output.append(
                                        "Nothing was saved."
                                );

                                statusLabel.setText(
                                        "Revision has insufficient capacity"
                                );
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showPlanError(
                                    outputArea,
                                    "Plan adaptation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showPlanError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    // =====================================================
    // F10 - CONFIRM
    // =====================================================

    private void confirmPlanRevision(
            JTextArea outputArea) {

        if (!controller.hasPendingRevision()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no pending revision to confirm.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No pending revision"
            );

            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Save the pending study-plan revision?",
                        "Confirm Revision",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {

            statusLabel.setText(
                    "Confirmation cancelled"
            );

            return;
        }

        try {

            StudyPlan saved =
                    controller.confirmRevision();

            outputArea.setText(
                    "REVISION CONFIRMED\n"
                            + "==============================\n"
                            + formatStudyPlan(saved)
                            + "\n"
                            + "The revision is now saved.\n"
                            + "One-level undo is available."
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Plan revision confirmed"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    // =====================================================
    // F10 - REJECT
    // =====================================================

    private void rejectPlanRevision(
            JTextArea outputArea) {

        if (!controller.hasPendingRevision()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no pending revision to reject.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No pending revision"
            );

            return;
        }

        controller.rejectRevision();

        outputArea.setText(
                "REVISION REJECTED\n"
                        + "==============================\n\n"
                        + "The pending revision was discarded.\n"
                        + "The saved study plan was not changed."
        );

        statusLabel.setText(
                "Plan revision rejected"
        );
    }

    // =====================================================
    // F10 - UNDO
    // =====================================================

    private void undoPlanRevision(
            JTextArea outputArea) {

        if (!controller.canUndo()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no confirmed revision "
                            + "available to undo.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No revision available to undo"
            );

            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Undo the last confirmed revision?",
                        "Undo Revision",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {

            statusLabel.setText(
                    "Undo cancelled"
            );

            return;
        }

        try {

            StudyPlan restored =
                    controller.undoLastRevision();

            outputArea.setText(
                    "REVISION UNDONE\n"
                            + "==============================\n"
                            + formatStudyPlan(restored)
                            + "\n"
                            + "The previous schedule was restored "
                            + "as a new plan version."
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Last plan revision undone"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    // =====================================================
    // PLAN INPUT DIALOGS
    // =====================================================

    private PlanInput requestPlanInput(
            String title) {

        String examText =
                JOptionPane.showInputDialog(
                        this,
                        "Exam date (YYYY-MM-DD):",
                        title,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (examText == null) {

            return null;
        }

        LocalDate examDate;

        try {

            examDate =
                    LocalDate.parse(
                            examText.trim()
                    );

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "Exam date must use YYYY-MM-DD."
            );
        }

        if (!examDate.isAfter(
                LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Exam date must be in the future."
            );
        }

        String dayCountText =
                JOptionPane.showInputDialog(
                        this,
                        "Number of available study days:",
                        title,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (dayCountText == null) {

            return null;
        }

        int dayCount;

        try {

            dayCount =
                    Integer.parseInt(
                            dayCountText.trim()
                    );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Number of study days must be a number."
            );
        }

        if (dayCount <= 0) {

            throw new IllegalArgumentException(
                    "At least one available study day is required."
            );
        }

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        for (int i = 1;
             i <= dayCount;
             i++) {

            String dateText =
                    JOptionPane.showInputDialog(
                            this,
                            "Availability "
                                    + i
                                    + " date (YYYY-MM-DD):",
                            title,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (dateText == null) {

                return null;
            }

            LocalDate date;

            try {

                date =
                        LocalDate.parse(
                                dateText.trim()
                        );

            } catch (DateTimeParseException ex) {

                throw new IllegalArgumentException(
                        "Availability date must use YYYY-MM-DD."
                );
            }

            if (date.isBefore(
                    LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Availability cannot be in the past."
                );
            }

            if (!date.isBefore(
                    examDate)) {

                throw new IllegalArgumentException(
                        "Availability must be before the exam date."
                );
            }

            String minutesText =
                    JOptionPane.showInputDialog(
                            this,
                            "Available minutes on "
                                    + date
                                    + ":",
                            title,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (minutesText == null) {

                return null;
            }

            int minutes;

            try {

                minutes =
                        Integer.parseInt(
                                minutesText.trim()
                        );

            } catch (NumberFormatException ex) {

                throw new IllegalArgumentException(
                        "Available minutes must be a number."
                );
            }

            if (minutes <= 0) {

                throw new IllegalArgumentException(
                        "Available minutes must be positive."
                );
            }

            availability.add(
                    new AvailabilitySlot(
                            date,
                            minutes
                    )
            );
        }

        Object strategyChoice =
                JOptionPane.showInputDialog(
                        this,
                        "Choose scheduling strategy:",
                        title,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[] {
                                "Deadline",
                                "Weak Topic"
                        },
                        "Weak Topic"
                );

        if (strategyChoice == null) {

            return null;
        }

        SchedulingStrategy strategy;

        if ("Deadline".equals(
                strategyChoice.toString())) {

            strategy =
                    new DeadlineStrategy();

        } else {

            strategy =
                    new WeakTopicStrategy();
        }

        return new PlanInput(
                examDate,
                availability,
                strategy
        );
    }

    // =====================================================
    // PLAN DISPLAY
    // =====================================================

    private String formatStudyPlan(
            StudyPlan plan) {

        StringBuilder output =
                new StringBuilder();

        output.append(
                "\n"
        );

        output.append(
                "Plan ID: "
                        + plan.getId()
                        + "\n"
        );

        output.append(
                "Version: "
                        + plan.getVersion()
                        + "\n"
        );

        output.append(
                "Exam date: "
                        + plan.getExamDate()
                        + "\n"
        );

        output.append(
                "Strategy: "
                        + plan.getStrategyName()
                        + "\n\n"
        );

        if (plan.getSessions()
                .isEmpty()) {

            output.append(
                    "No sessions could be scheduled.\n"
            );

        } else {

            output.append(
                    "Scheduled sessions:\n\n"
            );

            for (StudySession session :
                    plan.getSessions()) {

                output.append(
                        "- "
                                + session.getDate()
                                + " | "
                                + session.getTopic()
                                + " | "
                                + session.getMinutes()
                                + " min\n"
                );
            }
        }

        output.append(
                "\n"
        );

        if (plan.isFeasible()) {

            output.append(
                    "Plan status: FEASIBLE\n"
            );

        } else {

            output.append(
                    "Plan status: INSUFFICIENT CAPACITY\n"
            );

            if (!plan.getUnscheduledTopics()
                    .isEmpty()) {

                output.append(
                        "\nUnscheduled work:\n"
                );

                for (String topic :
                        plan.getUnscheduledTopics()) {

                    output.append(
                            "- "
                                    + topic
                                    + "\n"
                    );
                }
            }
        }

        return output.toString();
    }

    // =====================================================
    // PLAN ERRORS
    // =====================================================

    private void showPlanInputError(
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Invalid Study Plan Input",
                JOptionPane.WARNING_MESSAGE
        );

        statusLabel.setText(
                "Invalid study-plan input"
        );
    }

    private void showPlanError(
            JTextArea outputArea,
            String message) {

        outputArea.setText(
                "STUDY PLAN OPERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "Study-plan operation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "Study Plan Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // F06 - FLASHCARDS
    // =====================================================

    private JPanel createFlashcardsPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Flashcards",
                        "Generate grounded flashcards from course material."
                );

        JButton generateButton =
                new JButton(
                        "Generate Flashcards"
                );

        generateButton.addActionListener(
                e -> showPlaceholder(
                        feature.outputArea,
                        "Flashcards",
                        "F06 will be connected next."
                )
        );

        addButtons(
                feature,
                generateButton
        );

        return feature.panel;
    }

    // =====================================================
    // F07 / F08 - QUIZ
    // =====================================================

    private JPanel createQuizPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Quiz",
                        "Generate and grade grounded multiple-choice quizzes."
                );

        JButton generateButton =
                new JButton(
                        "Generate Quiz"
                );

        JButton gradeButton =
                new JButton(
                        "Grade Quiz"
                );

        generateButton.addActionListener(
                e -> showPlaceholder(
                        feature.outputArea,
                        "Generate Quiz",
                        "F07 will be connected later."
                )
        );

        gradeButton.addActionListener(
                e -> showPlaceholder(
                        feature.outputArea,
                        "Grade Quiz",
                        "F08 will be connected later."
                )
        );

        addButtons(
                feature,
                generateButton,
                gradeButton
        );

        return feature.panel;
    }

    // =====================================================
    // F09 - PROGRESS
    // =====================================================

    private JPanel createProgressPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Progress",
                        "View topic accuracy and weak-topic analysis."
                );

        JButton progressButton =
                new JButton(
                        "View Progress"
                );

        progressButton.addActionListener(
                e -> showPlaceholder(
                        feature.outputArea,
                        "Course Progress",
                        "F09 will be connected later."
                )
        );

        addButtons(
                feature,
                progressButton
        );

        return feature.panel;
    }

    // =====================================================
    // SHARED FEATURE PANEL
    // =====================================================

    private FeaturePanel createFeaturePanel(
            String title,
            String description) {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        JPanel headingPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                1,
                                0,
                                5
                        )
                );

        JLabel heading =
                new JLabel(
                        title
                );

        heading.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        22
                )
        );

        JLabel descriptionLabel =
                new JLabel(
                        description
                );

        headingPanel.add(
                heading
        );

        headingPanel.add(
                descriptionLabel
        );

        JPanel buttonPanel =
                new JPanel(
                        new GridLayout(
                                0,
                                3,
                                10,
                                10
                        )
                );

        JPanel top =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        top.add(
                headingPanel,
                BorderLayout.NORTH
        );

        top.add(
                buttonPanel,
                BorderLayout.CENTER
        );

        panel.add(
                top,
                BorderLayout.NORTH
        );

        JTextArea outputArea =
                new JTextArea();

        outputArea.setEditable(
                false
        );

        outputArea.setLineWrap(
                true
        );

        outputArea.setWrapStyleWord(
                true
        );

        outputArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14
                )
        );

        outputArea.setText(
                "Select an action above."
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        outputArea
                );

        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        "Output"
                )
        );

        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return new FeaturePanel(
                panel,
                buttonPanel,
                outputArea
        );
    }

    private void addButtons(
            FeaturePanel feature,
            JButton... buttons) {

        for (JButton button :
                buttons) {

            button.setPreferredSize(
                    new Dimension(
                            180,
                            40
                    )
            );

            feature.buttonPanel.add(
                    button
            );
        }
    }

    // =====================================================
    // PLACEHOLDER
    // =====================================================

    private void showPlaceholder(
            JTextArea outputArea,
            String title,
            String message) {

        outputArea.setText(
                title
                        + "\n"
                        + "=============================="
                        + "\n\n"
                        + message
        );

        statusLabel.setText(
                title
        );
    }

    // =====================================================
    // AI ERROR
    // =====================================================

    private void showAITaskError(
            JTextArea outputArea,
            String message) {

        outputArea.setText(
                "AI OPERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "AI operation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "StudyMate AI Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // STATUS BAR
    // =====================================================

    private JPanel createStatusBar() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        10,
                        15
                )
        );

        statusLabel =
                new JLabel(
                        "Ready"
                );

        panel.add(
                statusLabel,
                BorderLayout.WEST
        );

        return panel;
    }

    // =====================================================
    // ERROR HELPER
    // =====================================================

    private String safeMessage(
            Throwable error) {

        if (error == null) {

            return "Unknown error.";
        }

        if (error.getMessage() != null
                && !error.getMessage().isBlank()) {

            return error.getMessage();
        }

        return error
                .getClass()
                .getSimpleName();
    }

    // =====================================================
    // INTERNAL GUI VALUES
    // =====================================================

    private static class FeaturePanel {

        private final JPanel panel;
        private final JPanel buttonPanel;
        private final JTextArea outputArea;

        private FeaturePanel(
                JPanel panel,
                JPanel buttonPanel,
                JTextArea outputArea) {

            this.panel =
                    panel;

            this.buttonPanel =
                    buttonPanel;

            this.outputArea =
                    outputArea;
        }
    }

    private static class PlanInput {

        private final LocalDate examDate;
        private final List<AvailabilitySlot> availability;
        private final SchedulingStrategy strategy;

        private PlanInput(
                LocalDate examDate,
                List<AvailabilitySlot> availability,
                SchedulingStrategy strategy) {

            this.examDate =
                    examDate;

            this.availability =
                    availability;

            this.strategy =
                    strategy;
        }
    }

    // =====================================================
    // GUI ENTRY POINT
    // =====================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    try {

                        UIManager.setLookAndFeel(
                                UIManager
                                        .getSystemLookAndFeelClassName()
                        );

                    } catch (Exception ignored) {
                    }

                    try {

                        StudyGUI gui =
                                new StudyGUI();

                        gui.setVisible(
                                true
                        );

                    } catch (Exception e) {

                        JOptionPane.showMessageDialog(
                                null,
                                "StudyMate could not start:\n"
                                        + (
                                                e.getMessage() == null
                                                        ? e.getClass()
                                                                .getSimpleName()
                                                        : e.getMessage()
                                        ),
                                "StudyMate Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
        );
    }
}