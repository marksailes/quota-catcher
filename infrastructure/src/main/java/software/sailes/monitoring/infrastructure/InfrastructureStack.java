package software.sailes.monitoring.infrastructure;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IFunction quotaCatcher = Function.Builder.create(this, "QuotaCatcher")
                .functionName("QuotaCatcher")
                .code(Code.fromAsset("../software/quota-catcher/HelloWorldFunction/target/quota-catcher-1.0.jar"))
                .handler("software.sailes.monitoring.QuotaCatcherHandler")
                .runtime(Runtime.JAVA_11)
                .memorySize(1024)
                .architecture(Architecture.ARM_64)
                .logRetention(RetentionDays.ONE_WEEK)
                .build();

        quotaCatcher.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("servicequotas:GetServiceQuota"))
                .resources(List.of("*"))
                .build());

        Rule rule = Rule.Builder.create(this, "OneMinuteSchedule")
                .schedule(Schedule.rate(Duration.minutes(1)))
                .build();
        rule.addTarget(new LambdaFunction(quotaCatcher));
    }
}
