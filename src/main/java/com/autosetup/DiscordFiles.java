package com.autosetup;

import com.zenith.command.api.CommandContext;
import com.zenith.command.api.DiscordCommandContext;
import com.zenith.discord.Embed;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Uploading and downloading setup.txt through Discord. */
public final class DiscordFiles {
    private static final int MAX_BYTES = 64 * 1024; // way more than any setup list needs

    private DiscordFiles() {}

    /** "autoSetup upload" with a .txt attached to the message. */
    public static void upload(CommandContext ctx) {
        Embed embed = ctx.getEmbed();
        if (!(ctx instanceof DiscordCommandContext discord)) {
            embed.title("Upload only works from Discord")
                .description("Send `.autoSetup upload` in Discord with setup.txt attached")
                .errorColor();
            return;
        }
        List<Message.Attachment> attachments = discord.getMessageReceivedEvent().getMessage().getAttachments();
        Message.Attachment file = attachments.stream()
            .filter(a -> "txt".equalsIgnoreCase(a.getFileExtension()))
            .findFirst()
            .orElse(null);
        if (file == null) {
            embed.title("No .txt file attached")
                .description("Attach your setup.txt to the same message as `.autoSetup upload`")
                .errorColor();
            return;
        }
        if (file.getSize() > MAX_BYTES) {
            embed.title("File too big")
                .description("setup.txt can be at most 64 KB")
                .errorColor();
            return;
        }
        try {
            byte[] bytes;
            try (InputStream in = file.getProxy().download().get(20, TimeUnit.SECONDS)) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) {
                embed.title("File too big").description("setup.txt can be at most 64 KB").errorColor();
                return;
            }
            String text = decode(bytes);
            if (text == null) {
                embed.title("That doesn't look like a text file")
                    .description("Save it as plain text (UTF-8) and try again")
                    .errorColor();
                return;
            }
            List<String> commands = SetupFile.replace(text);
            AutoSetupPlugin.LOG.info("AutoSetup: setup.txt replaced from Discord by {} ({} commands)",
                discord.getMessageReceivedEvent().getAuthor().getName(), commands.size());
            embed.title("setup.txt uploaded")
                .description(commands.size() + " commands. Use `autoSetup run` to apply them now, "
                    + "or `autoSetup list` to check them.\nThe old file was kept as setup.txt.bak")
                .successColor();
            long skipped = commands.stream().filter(AutoSetupPlugin::isSelf).count();
            if (skipped > 0) {
                embed.addField("Heads up", skipped + " autoSetup line(s) in the file will be skipped", false);
            }
        } catch (Exception e) {
            AutoSetupPlugin.LOG.warn("AutoSetup: upload failed", e);
            embed.title("Upload failed")
                .description(String.valueOf(e.getMessage()))
                .errorColor();
        }
    }

    /** "autoSetup download": the bot sends setup.txt back as a file. */
    public static void download(CommandContext ctx) {
        Embed embed = ctx.getEmbed();
        if (!(ctx instanceof DiscordCommandContext discord)) {
            embed.title("Download only works from Discord")
                .description("Your setup.txt is at `" + SetupFile.location() + "`")
                .errorColor();
            return;
        }
        if (!SetupFile.exists()) {
            embed.title("No setup.txt")
                .description("Upload one with `.autoSetup upload` and the file attached")
                .errorColor();
            return;
        }
        try {
            discord.getMessageReceivedEvent().getChannel()
                .sendFiles(FileUpload.fromData(SetupFile.PATH.toFile(), "setup.txt"))
                .queue();
            embed.title("Here's your setup.txt").primaryColor();
        } catch (Exception e) {
            AutoSetupPlugin.LOG.warn("AutoSetup: download failed", e);
            embed.title("Download failed").description(String.valueOf(e.getMessage())).errorColor();
        }
    }

    /** Strict UTF-8 decode, so a random binary file gets rejected instead of saved. */
    private static String decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
            if (text.startsWith("﻿")) text = text.substring(1);
            if (text.indexOf('\0') >= 0) return null;
            return text;
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
