package org.rajawali3d.loader.md5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.ATexture.TextureException;
import org.rajawali3d.materials.textures.NormalMapTexture;
import org.rajawali3d.materials.textures.SpecularMapTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.util.RajLog;

import android.graphics.BitmapFactory;

/**
 * This class parses MTR-format files, providing a means for MD5
 * models to specify diffuse/normal/specular maps rather than
 * simply a flat texture. MTR files may also contain custom
 * shader code; this feature is not supported here, though
 * some mechanism for parametrising specularity, diffuse level
 * etc may be added in the future.
 * 
 * See the following:
 * http://wiki.thedarkmod.com/index.php?title=Basic_Material_File
 * http://www.iddevnet.com/doom3/materials.php
 * 
 * [material] name/or/path
 * {
 * 		qer_editorimage path/to/ambient/texture
 * 		bumpmap         path/to/normal/map
 * 		diffusemap      path/to/diffuse/texture
 * 		specularmap     path/to/specular/map
 * }
 * 
 * @author Bernard Gorman (bernard.gorman@gmail.com)
 * 
 */
public class LoaderMD5Material
{
	private final static String AMBIENT_MAP = "qer_editorimage";
	private final static String SPECULAR_MAP = "specularmap";
	private final static String DIFFUSE_MAP = "diffusemap";
	private final static String NORMAL_MAP = "bumpmap";

	public LoaderMD5Material()
	{
		// nothing to do here
	}

	public Material parse(File mtrFile)
	{
		return parse(new Material(), mtrFile);
	}

	@SuppressWarnings("unused")
	public Material parse(Material mat, File mtrFile)
	{
		Map<String, String> props = new HashMap<String, String>();

		String matName = null;

		try
		{
			BufferedReader buffer = new BufferedReader(new FileReader(mtrFile));

			// first line is material name
			String line = buffer.readLine();
			String[] tokens = null;

			tokens = line.trim().split("[\\s\\/]+");

			// "material" keyword at start of line is optional
			matName = (tokens.length > 0 ? tokens[tokens.length-1] : null);

			// build property map from remainder of file
			while((line = buffer.readLine()) != null)
			{
				tokens = line.trim().split("\\s+");

				if(tokens.length > 1)
					props.put(tokens[0], tokens[1]);
			}

			buffer.close();
		}
		catch(Exception e)
		{
			RajLog.e("["+getClass().getCanonicalName()+"] Exception while reading MTR shader: " + e);
			return null;
		}

		// diffuse map takes precedence over ambient
		if(props.containsKey(DIFFUSE_MAP))
			props.remove(AMBIENT_MAP);

		for(Entry<String, String> entry : props.entrySet())
		{
			File texFile = new File(mtrFile.getParentFile(), entry.getValue());

			if(!texFile.exists())
			{
				RajLog.e("["+getClass().getCanonicalName()+"] Could not find texture file " + texFile.getAbsolutePath());
				continue;
			}

			String texFilePath = texFile.getAbsolutePath();
			String texName = cleanName(texFile.getName());

			ATexture tex =
				entry.getKey().equals(DIFFUSE_MAP) ? new Texture(texName, BitmapFactory.decodeFile(texFilePath)) :
				entry.getKey().equals(SPECULAR_MAP) ? new SpecularMapTexture(texName, BitmapFactory.decodeFile(texFilePath)) :
				entry.getKey().equals(NORMAL_MAP) ? new NormalMapTexture(texName, BitmapFactory.decodeFile(texFilePath)) :
				entry.getKey().equals(AMBIENT_MAP) ? new Texture(texName, BitmapFactory.decodeFile(texFilePath)) : null;

			try
			{
				mat.addTexture(tex);
			}
			catch (TextureException e)
			{
				RajLog.e("["+getClass().getCanonicalName()+"] Exception while adding texture: " + e);
			}
		}

		if(props.containsKey(DIFFUSE_MAP))
			mat.setDiffuseMethod(new DiffuseMethod.Lambert());

		if(props.containsKey(SPECULAR_MAP))
			mat.setSpecularMethod(new SpecularMethod.Phong());

		mat.enableLighting(props.containsKey(DIFFUSE_MAP) || props.containsKey(SPECULAR_MAP));
		mat.setColorInfluence(0);

		return mat;
	}

	private final static String TEX_PREFIX = "TEX_";

	private String cleanName(String name)
	{
		// if null, force generation of a new and valid name
		String clean = (name == null ? "" : name.replaceAll("\\W", ""));

		if(clean.length() == 0 || Character.isDigit(clean.charAt(0)))
			clean = TEX_PREFIX + UUID.randomUUID().toString().replaceAll("\\W", "");

		return clean;
	}
}
