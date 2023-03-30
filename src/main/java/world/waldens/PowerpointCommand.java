package world.waldens;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

// Import everything in the CommandManager
import static net.minecraft.server.command.CommandManager.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class PowerpointCommand {

    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No sign present at the position."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(
            literal("powerpoint")
                .requires(source -> source.hasPermissionLevel(2))
                .then(
                    argument("pos", BlockPosArgumentType.blockPos())
                        .executes(ctx -> execute(
                            ctx.getSource(),
                            BlockPosArgumentType.getLoadedBlockPos(ctx, "pos"),
                            1
                        ))
                        .then(
                            argument("increment", IntegerArgumentType.integer())
                            .executes(ctx -> execute(
                                ctx.getSource(),
                                BlockPosArgumentType.getLoadedBlockPos(ctx, "pos"),
                                IntegerArgumentType.getInteger(ctx, "increment")
                            ))
                        )
                )
        );

    }

    private static int execute(ServerCommandSource source, BlockPos pos, Integer increment) throws CommandSyntaxException {
        ServerWorld serverWorld = source.getWorld();

        BlockEntity blockEntity = serverWorld.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity) {
            BlockState blockState = serverWorld.getBlockState(pos);
            SignBlockEntity sign = (SignBlockEntity)blockEntity;


            // only get first 3 lines of sign
            String url = String.join("", IntStream.range(0, 3).mapToObj((row) -> sign.getTextOnRow(row, false)).map(Text::getString).toArray(String[]::new));

            Integer currentFileID = 0;
            String regex = "(\\d+)(\\.png|\\.jpg)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);

            while (matcher.find()) {
                System.out.println("Full match: " + matcher.group(0) + " " + matcher.group(1) + " " + matcher.group(2));
                currentFileID = Integer.parseInt(matcher.group(1));
            }

            Integer newFileID = currentFileID + increment;
            if (newFileID <= 0) {
                newFileID = 1;
            }

            String modifiedUrl = url.replaceAll(regex, newFileID + "$2");


            String[] lines = breakLink(modifiedUrl);
            for (int i = 0; i < 3; i++) {
                sign.setTextOnRow(i, Text.of(lines[i]));
            }

            sign.markDirty();

            serverWorld.updateListeners(pos, blockState, blockState, Block.NOTIFY_ALL);
        }
        else {
            throw FAILED_EXCEPTION.create();
        }

        // source.sendFeedback(Text.translatable("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }
    
    private static String[] breakLink(String link) {
        Text linkText = Text.of(link);
        String[] brokenLink = new String[3];
        Text line0Text = linkText;
        int line0width = line0Text.getString().length();
        while (line0width > 15) {
            --line0width;
            line0Text = Text.of(line0Text.getString().substring(0,line0width));
        }
        brokenLink[0] = line0Text.getString();
        Text line1Text = Text.of(linkText.getString().substring(line0width));
        int line1width = line1Text.getString().length();
        while (line1width > 15) {
            --line1width;
            line1Text = Text.of(line1Text.getString().substring(0,line1width));
        }
        brokenLink[1] = line1Text.getString();
        Text line2Text = Text.of(linkText.getString().substring(line0width + line1width));
        // int line2width = line2Text.getString().length();
        // if (!PictureSignConfig.exceedVanillaLineLength)
        //     while (this.client.textRenderer.getWidth(line2Text) >= 90) {
        //         --line2width;
        //         line2Text = Text.of(line2Text.getString().substring(0,line2width));
        //     }
        // }
        brokenLink[2] = line2Text.getString();

        return brokenLink;
    }
}
