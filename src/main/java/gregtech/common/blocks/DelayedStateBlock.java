package gregtech.common.blocks;

import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * This class allows lazy initialization of block state of block
 * Useful when you need some parameters from constructor to construct a BlockStateContainer
 * All child classes must call initBlockState() in their constructors
 */
public abstract class DelayedStateBlock extends Block {

    public DelayedStateBlock(Material materialIn) {
        super(materialIn);
    }

    @Override
    protected final BlockStateContainer createBlockState() {
        return new BlockStateContainer(this);
    }

    protected void initBlockState() {
        BlockStateContainer stateContainer = createStateContainer();
        ObfuscationReflectionHelper.setPrivateValue(Block.class, this, stateContainer, 21); //this.stateContainer
        setDefaultState(stateContainer.getBaseState());
    }

    protected abstract BlockStateContainer createStateContainer();

}
