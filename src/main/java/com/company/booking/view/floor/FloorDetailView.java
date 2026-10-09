package com.company.booking.view.floor;

import com.company.booking.entity.Floor;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;


@Route(value = "floors/:id", layout = MainView.class)
@ViewController(id = "Floor_.detail")
@ViewDescriptor(path = "floor-detail-view.xml")
@EditedEntityContainer("floorDc")
public class FloorDetailView extends StandardDetailView<Floor> {

    @ViewComponent
    private JmixImage<Object> planImage;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        updatePlanImage();
    }

    @Subscribe(id = "floorDc", target = Target.DATA_CONTAINER)
    public void onFloorDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Floor> event) {
        if ("plan".equals(event.getProperty())) {
            updatePlanImage();
        }
    }

    private void updatePlanImage() {
        // Without a plan the image would render as a broken picture
        planImage.setVisible(getEditedEntity().getPlan() != null);
    }
}
