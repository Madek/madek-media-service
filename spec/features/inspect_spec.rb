describe "Inspectors", type: :feature do

  context "CRUD" do

    context "as a system admin" do

      let(:user) { create(:user, :with_system_admin_role) }

      let(:inspector_config) do
        YAML.load_file(
          PROJECT_DIR.join("inspector", "config.yml")
        ).with_indifferent_access
      end

      let(:media_files_dir) do
        PROJECT_DIR.join("spec/support/files")
      end

      before {sign_in}

      context do

        let!(:store) { create(:media_store, :database, :with_users, users: [user]) }

        it 'add an Inspector, upload a jpg', pending: true do

          # Settings
          visit '/media-service/'
          click_on 'Settings'
          click_on 'Edit'
          fill_in 'upload_max_part_size', with: (100 * 1024)
          fill_in 'upload_min_part_size', with: 1024
          click_on 'Save'


          # Inspector
          visit '/media-service/inspectors/'
          click_on 'Create'
          fill_in 'id', with: inspector_config[:id]
          check 'enabled'
          fill_in 'public_key', with: inspector_config[:'internal-key'][:'public-key']
          click_on 'Save'
          expect(page).to have_content 'Inspectors'
          expect(page).to have_content inspector_config[:id]


          # Upload
          visit '/media-service/'
          click_on 'Uploads'
          within "#uploads-page .form" do
            attach_file nil, media_files_dir.join('AnonPhoto.jpg')
          end

          binding.pry

        end

      end

    end
  end
end
