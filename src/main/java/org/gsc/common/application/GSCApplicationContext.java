package org.gsc.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.db.Manager;

public class GSCApplicationContext extends AnnotationConfigApplicationContext {

    public GSCApplicationContext() {
    }

    public GSCApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    public GSCApplicationContext(Class<?>... annotatedClasses) {
        super(annotatedClasses);
    }

    public GSCApplicationContext(String... basePackages) {
        super(basePackages);
    }

    @Override
    public void destroy() {

        Manager dbManager = getBean(Manager.class);
        dbManager.stopRepushThread();

        super.destroy();
    }
}
