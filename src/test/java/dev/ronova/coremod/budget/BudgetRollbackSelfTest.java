package dev.ronova.coremod.budget;

/** 7.14D enabled/default/invalid/legacy selection gate. */
public final class BudgetRollbackSelfTest {
    private BudgetRollbackSelfTest() {}

    public static void main(String[] args) {
        verifiesDefaultAndExplicitSelections();
        verifiesInvalidValuesRollBackToLegacy();
        verifiesPrecedenceAndWarningMetadata();
        verifiesGovernorConsumesTheSelection();
        System.out.println("BudgetRollbackSelfTest: PASS");
    }

    private static void verifiesDefaultAndExplicitSelections() {
        BudgetSplitFlags.Selection defaults = BudgetSplitFlags.resolve(
            ignored -> null, ignored -> null
        );
        require(defaults.enabled() && defaults.source() == BudgetSplitFlags.Source.DEFAULT
                && !defaults.hasWarning(),
            "7.14D must default to enabled without a warning");

        BudgetSplitFlags.Selection environment = BudgetSplitFlags.resolve(
            ignored -> null, ignored -> " FaLsE "
        );
        require(!environment.enabled()
                && environment.source() == BudgetSplitFlags.Source.ENVIRONMENT
                && !environment.hasWarning(),
            "valid environment rollback selection was not honored");

        BudgetSplitFlags.Selection property = BudgetSplitFlags.resolve(
            ignored -> " TrUe ", ignored -> "false"
        );
        require(property.enabled()
                && property.source() == BudgetSplitFlags.Source.JVM_PROPERTY
                && property.hasWarning(),
            "JVM property precedence or conflict warning is incorrect");
    }

    private static void verifiesInvalidValuesRollBackToLegacy() {
        BudgetSplitFlags.Selection invalidProperty = BudgetSplitFlags.resolve(
            ignored -> "enabled", ignored -> "true"
        );
        require(!invalidProperty.enabled()
                && invalidProperty.source() == BudgetSplitFlags.Source.INVALID_JVM_PROPERTY
                && invalidProperty.hasWarning()
                && invalidProperty.warning().contains("LEGACY"),
            "invalid JVM property did not select legacy scheduling");

        BudgetSplitFlags.Selection invalidEnvironment = BudgetSplitFlags.resolve(
            ignored -> null, ignored -> "maybe"
        );
        require(!invalidEnvironment.enabled()
                && invalidEnvironment.source() == BudgetSplitFlags.Source.INVALID_ENVIRONMENT
                && invalidEnvironment.hasWarning()
                && invalidEnvironment.warning().contains("LEGACY"),
            "invalid environment value did not select legacy scheduling");
    }

    private static void verifiesPrecedenceAndWarningMetadata() {
        BudgetSplitFlags.Selection matching = BudgetSplitFlags.resolve(
            ignored -> "true", ignored -> " TRUE "
        );
        require(matching.enabled()
                && matching.source() == BudgetSplitFlags.Source.JVM_PROPERTY
                && !matching.hasWarning(),
            "matching property and environment values produced a false warning");

        BudgetSplitFlags.Selection propertyFalse = BudgetSplitFlags.resolve(
            ignored -> "false", ignored -> "true"
        );
        require(!propertyFalse.enabled()
                && propertyFalse.warning().contains(BudgetSplitFlags.PROPERTY)
                && propertyFalse.warning().contains(BudgetSplitFlags.ENVIRONMENT),
            "conflicting flag warning did not identify both sources");
        require(BudgetSplitFlags.PROPERTY.equals("ronova.714d.frameBudget.enabled")
                && BudgetSplitFlags.ENVIRONMENT.equals("RONOVA_714D_FRAME_BUDGET_ENABLED"),
            "7.14D rollback keys changed");
    }

    private static void verifiesGovernorConsumesTheSelection() {
        FrameBudgetGovernor bounded = new FrameBudgetGovernor(
            new BudgetSplitFlags.Selection(true, BudgetSplitFlags.Source.DEFAULT, null)
        );
        FrameBudgetGovernor legacy = new FrameBudgetGovernor(
            new BudgetSplitFlags.Selection(
                false, BudgetSplitFlags.Source.INVALID_JVM_PROPERTY, "legacy"
            )
        );
        require(bounded.enabled() && bounded.selection().enabled(),
            "bounded governor ignored its selection");
        require(!legacy.enabled() && !legacy.selection().enabled(),
            "legacy governor ignored its selection");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
