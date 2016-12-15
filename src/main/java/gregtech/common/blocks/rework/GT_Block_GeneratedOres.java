package gregtech.common.blocks.rework;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneTypes;
import gregtech.api.items.GT_Generic_Block;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.common.render.RenderGeneratedOres;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GT_Block_GeneratedOres extends GT_Generic_Block {

    public static final int MATERIALS_META_OFFSET = Math.min(16, StoneTypes.mTypes.length);
    public static final int MATERIALS_PER_BLOCK = Math.max(1, 16 / MATERIALS_META_OFFSET);


    private static int sNextId;

    public static IBlockState[][] sGeneratedBlocks;
    public static IBlockState[][] sGeneratedSmallBlocks;

    public static void doOreThings() {
        System.out.println("MATERIALS META OFFSET: " + MATERIALS_META_OFFSET);
        System.out.println("MATERIALS/BLOCK: " + MATERIALS_PER_BLOCK);
        sGeneratedBlocks = new IBlockState[GregTech_API.sGeneratedMaterials.length][];
        sGeneratedSmallBlocks = new IBlockState[GregTech_API.sGeneratedMaterials.length][];
        Materials[] lastMats = new Materials[MATERIALS_PER_BLOCK];
        int length = 0;
        for(Materials aMaterial : Materials.values()) {
            if(aMaterial != null && aMaterial.mMetaItemSubID >= 0 && (aMaterial.mTypes & 0x08) != 0) {
                lastMats[length++] = aMaterial;
                if(length == MATERIALS_PER_BLOCK) {
                    new GT_Block_GeneratedOres(lastMats, false);
                    new GT_Block_GeneratedOres(lastMats, true);
                    lastMats = new Materials[MATERIALS_PER_BLOCK];
                    length = 0;
                }
            }
        }
        if(length != 0) {
            for(int i = 0; i < MATERIALS_PER_BLOCK; i++) {
                if(lastMats[i] == null)
                    lastMats[i] = Materials.Air;
            }
            new GT_Block_GeneratedOres(lastMats, false);
            new GT_Block_GeneratedOres(lastMats, true);
        }
        System.out.println("ORE BLOCKS REGISTERED: " + sNextId);
    }

    public static boolean setOreBlock(World world, BlockPos pos, int materialSubId, boolean small) {
        IBlockState prevState = world.getBlockState(pos);
        Block block = prevState.getBlock();
        int metadata = block.getMetaFromState(prevState);

        int variantId = 0;

        if(block == Blocks.STONE) {
            variantId = StoneTypes.STONE.mId;
        } else if(block == GregTech_API.sBlockGranites) {
            if(metadata == 0)
                variantId = StoneTypes.BLACK_GRANITE.mId;
            else if(metadata == 8) variantId = StoneTypes.RED_GRANITE.mId;
        } else if(block == GregTech_API.sBlockStones) {
            if(metadata == 0)
                variantId = StoneTypes.MARBLE.mId;
            else if(metadata == 8) variantId = StoneTypes.BASALT.mId;
        } else if(block == Blocks.NETHERRACK) {
            variantId = StoneTypes.NETHERRACK.mId;
        } else if(block == Blocks.END_STONE) {
            variantId = StoneTypes.ENDSTONE.mId;
        } else {
            return false;
        }

        IBlockState blockState = (small ? sGeneratedSmallBlocks : sGeneratedBlocks)[materialSubId][variantId];
        return world.setBlockState(pos, blockState);
    }

    public final Materials[] mMaterials;
    public final boolean mSmall;
    public int mId;

    protected GT_Block_GeneratedOres(Materials[] materials, boolean small) {
        super(GT_Item_GeneratedOres.class, "gt.blockores." + (sNextId), Material.ROCK);
        this.mId = sNextId;
        this.mMaterials = materials;
        this.mSmall = small;
        for(int i = 0; i < materials.length; i++) {
            Materials aMaterial = materials[i];
            IBlockState[] variants = new IBlockState[StoneTypes.mTypes.length];
            for(int j = 0; j < StoneTypes.mTypes.length; j++) {
                variants[j] = getStateFromMeta(i * MATERIALS_META_OFFSET + j);
                ItemStack itemStack = new ItemStack(this, 1, i * MATERIALS_META_OFFSET + j);
                GT_OreDictUnificator.registerOre(StoneTypes.mTypes[j].processingPrefix.get(aMaterial), itemStack);
                GT_LanguageManager.addStringLocalization(mUnlocalizedName + "." + itemStack.getItemDamage() + ".name", (small ? "Small " : "") + getLocalizedName(aMaterial));
            }
            (small ? sGeneratedSmallBlocks : sGeneratedBlocks)[aMaterial.mMetaItemSubID] = variants;
        }
        setSoundType(SoundType.STONE);
        setCreativeTab(GregTech_API.TAB_GREGTECH_ORES);
        sNextId++;
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for(int i = 0; i < mMaterials.length; i++) {
            for(int j = 0; j < StoneTypes.mTypes.length; j++) {
                list.add(new ItemStack(this, 1, i * 7 + j));
            }
        }
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        return "pickaxe";
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return this.mMaterials[state.getValue(METADATA) / 5].mToolQuality;
    }

    @Override
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
        return 1.0F + getHarvestLevel(blockState);
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return 1.0F + getHarvestLevel(world.getBlockState(pos));
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false; //never allow silk touch
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> rList = new ArrayList<>();
        int aMetaData = state.getValue(METADATA);
        if (aMetaData <= 0) {
            rList.add(new ItemStack(Blocks.COBBLESTONE, 1, 0));
            return rList;
        }
        if (!mSmall) {
            rList.add(new ItemStack(this, 1, aMetaData));
            return rList;
        }
        Materials aMaterial = GregTech_API.sGeneratedMaterials[(aMetaData % 1000)];
        if (aMaterial != null) {
            Random tRandom = new Random(pos.hashCode());
            ArrayList<ItemStack> tSelector = new ArrayList<>();

            ItemStack tStack = GT_OreDictUnificator.get(OrePrefixes.gemExquisite, aMaterial, GT_OreDictUnificator.get(OrePrefixes.gem, aMaterial, 1L), 1L);
            if (tStack != null) {
                for (int i = 0; i < 1; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.gemFlawless, aMaterial, GT_OreDictUnificator.get(OrePrefixes.gem, aMaterial, 1L), 1L);
            if (tStack != null) {
                for (int i = 0; i < 2; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.gem, aMaterial, 1L);
            if (tStack != null) {
                for (int i = 0; i < 12; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.gemFlawed, aMaterial, GT_OreDictUnificator.get(OrePrefixes.crushed, aMaterial, 1L), 1L);
            if (tStack != null) {
                for (int i = 0; i < 5; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.crushed, aMaterial, 1L);
            if (tStack != null) {
                for (int i = 0; i < 10; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.gemChipped, aMaterial, GT_OreDictUnificator.get(OrePrefixes.dustImpure, aMaterial, 1L), 1L);
            if (tStack != null) {
                for (int i = 0; i < 5; i++) {
                    tSelector.add(tStack);
                }
            }
            tStack = GT_OreDictUnificator.get(OrePrefixes.dustImpure, aMaterial, 1L);
            if (tStack != null) {
                for (int i = 0; i < 10; i++) {
                    tSelector.add(tStack);
                }
            }
            if (tSelector.size() > 0) {
                rList.add(GT_Utility.copyAmount(1L, tSelector.get(tRandom.nextInt(tSelector.size()))));
            }
        }
        return rList;
    }

    public String getLocalizedName(Materials aMaterial) {
        switch (aMaterial) {

            case InfusedAir:
            case InfusedDull:
            case InfusedEarth:
            case InfusedEntropy:
            case InfusedFire:
            case InfusedOrder:
            case InfusedVis:
            case InfusedWater:
                return aMaterial.mDefaultLocalName + " Infused Stone";

            case Vermiculite:
            case Bentonite:
            case Kaolinite:
            case Talc:
            case BasalticMineralSand:
            case GraniticMineralSand:
            case GlauconiteSand:
            case CassiteriteSand:
            case GarnetSand:
            case QuartzSand:
            case Pitchblende:
            case FullersEarth:
                return aMaterial.mDefaultLocalName;

            default:
                return aMaterial.mDefaultLocalName + OrePrefixes.ore.mLocalizedMaterialPost;
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return RenderGeneratedOres.INSTANCE.renderType;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
