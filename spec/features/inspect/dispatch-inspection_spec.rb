describe "JPG inspection", type: :feature do

  context 'as a system admin, ' do

    include_context :signed_in_as_a_system_admin

    context 'a configured inspector' do
      include_context :inspector

      context 'uploaded image' do
        include_context :uploaded_jpg

        example 'an inspection has been created and is beeing dispatched' do
          expect(current_url).to match '/media-service/originals/(.+)'
          upload_id = current_url.match(/.*originals\/(.*)/)[1]
          inspection = Inspection.where(media_file_id: upload_id).first
          expect(inspection).to be
          wait_until(30) do
            inspection.reload[:state] == 'dispatched'
          end
          binding.pry
        end

      end

    end

  end
end
