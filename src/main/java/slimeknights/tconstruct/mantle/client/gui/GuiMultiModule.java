package slimeknights.tconstruct.mantle.client.gui;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import slimeknights.tconstruct.mantle.inventory.ContainerMultiModule;
import slimeknights.tconstruct.mantle.inventory.SlotWrapper;

@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
// todo: NEI
public class GuiMultiModule extends GuiContainer { //implements INEIGuiHandler {

  // NEI-stuff >:(
  private static Field NEI_Manager;

  static {
    try {
      NEI_Manager = GuiContainer.class.getDeclaredField("manager");
    } catch(NoSuchFieldException e) {
      NEI_Manager = null;
    }
  }

  protected List<GuiModule> modules = Lists.newArrayList();

  public int cornerX;
  public int cornerY;
  public int realWidth;
  public int realHeight;

  public GuiMultiModule(ContainerMultiModule container) {
    super(container);

    realWidth = -1;
    realHeight = -1;
  }

  protected void addModule(GuiModule module) {
    modules.add(module);
  }

  public List<Rectangle> getModuleAreas() {
    List<Rectangle> areas = new ArrayList<Rectangle>(modules.size());
    for(GuiModule module : modules) {
      areas.add(module.getArea());
    }
    return areas;
  }

  @Override
  public void initGui() {
    if(realWidth > -1) {
      // has to be reset before calling initGui so the position is getting retained
      xSize = realWidth;
      ySize = realHeight;
    }
    super.initGui();

    this.cornerX = this.guiLeft;
    this.cornerY = this.guiTop;
    this.realWidth = xSize;
    this.realHeight = ySize;

    for(GuiModule module : modules) {
      updateSubmodule(module);
    }

    //this.guiLeft = this.guiTop = 0;
    //this.xSize = width;
    //this.ySize = height;
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    for(GuiModule module : modules) {
      module.handleDrawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    }
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    drawContainerName();
    drawPlayerInventoryName();

    for(GuiModule module : modules) {
      // set correct state for the module
      GlStateManager.pushMatrix();
      GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
      GlStateManager.translate(module.getGuiLeft(), module.getGuiTop(), 0.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      module.handleDrawGuiContainerForegroundLayer(mouseX, mouseY);
      GlStateManager.popMatrix();
    }
  }

  protected void drawBackground(ResourceLocation background) {
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.getTextureManager().bindTexture(background);
    this.drawTexturedModalRect(cornerX, cornerY, 0, 0, realWidth, realHeight);
  }

  protected void drawContainerName() {
    ContainerMultiModule multiContainer = (ContainerMultiModule) this.inventorySlots;
    String localizedName = multiContainer.getInventoryDisplayName();
    if(localizedName != null) {
      this.fontRenderer.drawString(localizedName, 8, 6, 0x404040);
    }
  }

  protected void drawPlayerInventoryName() {
    String localizedName = Minecraft.getMinecraft().player.inventory.getDisplayName().getUnformattedText();
    this.fontRenderer.drawString(localizedName, 8, this.ySize - 96 + 2, 0x404040);
  }

  @Override
  public void setWorldAndResolution(Minecraft mc, int width, int height) {
    super.setWorldAndResolution(mc, width, height);

    // workaround for NEIs ASM hax. sigh.
    try {
      for(GuiModule module : modules) {
        module.setWorldAndResolution(mc, width, height);
        if(NEI_Manager != null) {
          NEI_Manager.set(module, NEI_Manager.get(this));
        }
        updateSubmodule(module);
      }
    } catch(IllegalAccessException e) {
     // TConstruct.logger.error(e);
    }
  }

  @Override
  public void onResize(@Nonnull Minecraft mc, int width, int height) {
    super.onResize(mc, width, height);

    for(GuiModule module : modules) {
      module.onResize(mc, width, height);
      updateSubmodule(module);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    this.drawDefaultBackground();
    int oldX = guiLeft;
    int oldY = guiTop;
    int oldW = xSize;
    int oldH = ySize;

    guiLeft = cornerX;
    guiTop = cornerY;
    xSize = realWidth;
    ySize = realHeight;
    super.drawScreen(mouseX, mouseY, partialTicks);
    this.renderHoveredToolTip(mouseX, mouseY);
    guiLeft = oldX;
    guiTop = oldY;
    xSize = oldW;
    ySize = oldH;
  }


  // needed to get the correct slot on clicking
  @Override
  protected boolean isPointInRegion(int left, int top, int right, int bottom, int pointX, int pointY) {
    pointX -= this.cornerX;
    pointY -= this.cornerY;
    return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
  }

  protected void updateSubmodule(GuiModule module) {
    module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

    if(module.getGuiLeft() < this.guiLeft) {
      this.xSize += this.guiLeft - module.getGuiLeft();
      this.guiLeft = module.getGuiLeft();
    }
    if(module.getGuiTop() < this.guiTop) {
      this.ySize += this.guiTop - module.getGuiTop();
      this.guiTop = module.getGuiTop();
    }
    if(module.guiRight() > this.guiLeft + this.xSize) {
      xSize = module.guiRight() - this.guiLeft;
    }
    if(module.guiBottom() > this.guiTop + this.ySize) {
      ySize = module.guiBottom() - this.guiTop;
    }
  }

  @Override
  public void drawSlot(Slot slotIn) {
    GuiModule module = getModuleForSlot(slotIn.slotNumber);

    if(module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if(slotIn instanceof SlotWrapper) {
        slot = ((SlotWrapper) slotIn).parent;
      }
      if(!module.shouldDrawSlot(slot)) {
        return;
      }
    }

    // update slot positions
    if(slotIn instanceof SlotWrapper) {
      slotIn.xPos = ((SlotWrapper) slotIn).parent.xPos;
      slotIn.yPos = ((SlotWrapper) slotIn).parent.yPos;
    }

    super.drawSlot(slotIn);
  }

  @Override
  public boolean isMouseOverSlot(Slot slotIn, int mouseX, int mouseY) {
    GuiModule module = getModuleForSlot(slotIn.slotNumber);

    // mouse inside the module of the slot?
    if(module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if(slotIn instanceof SlotWrapper) {
        slot = ((SlotWrapper) slotIn).parent;
      }
      if(!module.shouldDrawSlot(slot)) {
        return false;
      }
    }

    return super.isMouseOverSlot(slotIn, mouseX, mouseY);
  }


  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    GuiModule module = getModuleForPoint(mouseX, mouseY);
    if(module != null) {
      if(module.handleMouseClicked(mouseX, mouseY, mouseButton)) {
        return;
      }
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    GuiModule module = getModuleForPoint(mouseX, mouseY);
    if(module != null) {
      if(module.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
        return;
      }
    }

    super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
  }

  @Override
  protected void mouseReleased(int mouseX, int mouseY, int state) {
    GuiModule module = getModuleForPoint(mouseX, mouseY);
    if(module != null) {
      if(module.handleMouseReleased(mouseX, mouseY, state)) {
        return;
      }
    }

    super.mouseReleased(mouseX, mouseY, state);
  }

  protected GuiModule getModuleForPoint(int x, int y) {
    for(GuiModule module : modules) {
      if(this.isPointInRegion(module.getGuiLeft(), module.getGuiTop(), module.guiRight(), module.guiBottom(),
                              x + this.cornerX, y + this.cornerY)) {
        return module;
      }
    }

    return null;
  }

  protected GuiModule getModuleForSlot(int slotNumber) {
    return getModuleForContainer(getContainer().getSlotContainer(slotNumber));
  }

  protected GuiModule getModuleForContainer(Container container) {
    for(GuiModule module : modules) {
      if(module.inventorySlots == container) {
        return module;
      }
    }

    return null;
  }

  protected ContainerMultiModule getContainer() {
    return (ContainerMultiModule) inventorySlots;
  }

  /* NEI INTEGRATION */
  // todo: NEI
/*
  @Override
  @Optional.Method(modid = "NotEnoughItems")
  public VisiblityData modifyVisiblity(GuiContainer guiContainer, VisiblityData visiblityData) {
    int x = LayoutManager.stateButtons[0].x + LayoutManager.stateButtons[0].w;
    int x2 = LayoutManager.timeButtons[3].x + LayoutManager.timeButtons[3].w;
    int y2 = LayoutManager.heal.y + LayoutManager.heal.h;


    for(GuiModule module : modules) {
      if(x > module.guiLeft) {
        visiblityData.showStateButtons = false;
      }
      if(x2 > module.guiLeft && y2 > module.guiTop) {
        visiblityData.showUtilityButtons = false;
      }
    }

    return visiblityData;
  }

  @Override
  @Optional.Method(modid = "NotEnoughItems")
  public Iterable<Integer> getItemSpawnSlots(GuiContainer guiContainer, ItemStack itemStack) {
    return null;
  }

  @Override
  @Optional.Method(modid = "NotEnoughItems")
  public List<TaggedInventoryArea> getInventoryAreas(GuiContainer guiContainer) {
    return Collections.EMPTY_LIST;
  }

  @Override
  @Optional.Method(modid = "NotEnoughItems")
  public boolean handleDragNDrop(GuiContainer guiContainer, int x, int y, ItemStack itemStack, int k) {
    return false;
  }

  @Override
  @Optional.Method(modid = "NotEnoughItems")
  public boolean hideItemPanelSlot(GuiContainer guiContainer, int x, int y, int w, int h) {
    for(GuiModule module : modules) {
      // check if the panel overlaps the module (check totally not stolen from stackoverflow)
      if(module.guiLeft < x + w && module.guiRight() > x &&
         module.guiTop < y + h && module.guiBottom() > y) {
        return true;
      }
    }

    return false;
  }
  */
}
