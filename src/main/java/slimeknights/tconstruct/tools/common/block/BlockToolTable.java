package slimeknights.tconstruct.tools.common.block;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

import javax.annotation.Nonnull;

import slimeknights.tconstruct.mantle.inventory.BaseContainer;
import slimeknights.tconstruct.CraftingStation;
import slimeknights.tconstruct.shared.block.BlockTable;
import slimeknights.tconstruct.tools.common.tileentity.TileCraftingStation;

public class BlockToolTable extends BlockTable implements ITinkerStationBlock {

  public static final PropertyEnum<TableTypes> TABLES = PropertyEnum.create("type", TableTypes.class);

  public BlockToolTable() {
    super(Material.WOOD);
    this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);

    this.setSoundType(SoundType.WOOD);
    this.setResistance(5f);
    this.setHardness(1f);

    // set axe as effective tool for all variants
    this.setHarvestLevel("axe", 0);
  }

  @Nonnull
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    switch(TableTypes.fromMeta(meta)) {
      case CraftingStation:
        return new TileCraftingStation();
      default:
        return super.createNewTileEntity(worldIn, meta);
    }
  }

  @Override
  public boolean openGui(EntityPlayer player, World world, BlockPos pos) {
    if(!world.isRemote) {
      player.openGui(CraftingStation.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
      if(player.openContainer instanceof BaseContainer) {
        ((BaseContainer) player.openContainer).syncOnOpen((EntityPlayerMP) player);
      }
    }
    return true;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float clickX, float clickY, float clickZ) {
    TileEntity te = world.getTileEntity(pos);
    ItemStack heldItem = player.inventory.getCurrentItem();
    heldItem.isEmpty();

    return super.onBlockActivated(world, pos, state, player, hand, side, clickX, clickY, clickZ);
  }

  @SideOnly(Side.CLIENT)
  @Override
  public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
    // crafting station is boring
    list.add(new ItemStack(this, 1, TableTypes.CraftingStation.meta));

  }

  @Override
  protected boolean keepInventory(IBlockState state) {
    return false;
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new ExtendedBlockState(this, new IProperty[]{TABLES}, new IUnlistedProperty[]{TEXTURE, INVENTORY, FACING});
  }

  @Nonnull
  @Override
  public IBlockState getStateFromMeta(int meta) {
    return this.getDefaultState().withProperty(TABLES, TableTypes.fromMeta(meta));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return (state.getValue(TABLES)).meta;
  }

  /* Bounds */
  private static ImmutableList<AxisAlignedBB> BOUNDS_Chest = ImmutableList.of(
      new AxisAlignedBB(0, 0.9375, 0, 1, 1, 1), // top
      new AxisAlignedBB(0.0625, 0.1875, 0.0625, 0.9375, 1, 0.9375), // middle
      new AxisAlignedBB(0.03125, 0, 0.03125, 0.15625, 0.75, 0.15625),
      new AxisAlignedBB(0.84375, 0, 0.03125, 0.96875, 0.75, 0.15625),
      new AxisAlignedBB(0.84375, 0, 0.84375, 0.96875, 0.75, 0.96875),
      new AxisAlignedBB(0.03125, 0, 0.84375, 0.15625, 0.75, 0.96875)
  );

  @Override
  public RayTraceResult collisionRayTrace(IBlockState blockState, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
    if(blockState.getValue(TABLES).isChest) {
      return raytraceMultiAABB(BOUNDS_Chest, pos, start, end);
    }

    return super.collisionRayTrace(blockState, worldIn, pos, start, end);
  }

  @Override
  public int getGuiNumber(IBlockState state) {
    switch(state.getValue(TABLES)) {
      case CraftingStation:
        return 50;
      default:
        return 0;
    }
  }

  public enum TableTypes implements IStringSerializable {
    CraftingStation;


    TableTypes() {
      meta = this.ordinal();
      this.isChest = false;
    }

    TableTypes(boolean chest) {
      meta = this.ordinal();
      this.isChest = chest;
    }

    public final int meta;
    public final boolean isChest;

    public static TableTypes fromMeta(int meta) {
      if(meta < 0 || meta >= values().length) {
        meta = 0;
      }

      return values()[meta];
    }

    @Override
    public String getName() {
      return this.toString().toLowerCase(Locale.US);
    }
  }
}
